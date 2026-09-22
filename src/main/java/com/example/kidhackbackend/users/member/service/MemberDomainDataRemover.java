package com.example.kidhackbackend.users.member.service;

import com.example.kidhackbackend.users.appcontrol.repository.AppControlRuleRepository;
import com.example.kidhackbackend.users.appcontrol.repository.InstalledAppRepository;
import com.example.kidhackbackend.users.appcontrol.repository.ScreenTimeSettingRepository;
import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import com.example.kidhackbackend.users.chat.repository.ChatMessageRepository;
import com.example.kidhackbackend.users.chat.repository.ChatRoomRepository;
import com.example.kidhackbackend.users.device.repository.DeviceEventRepository;
import com.example.kidhackbackend.users.efin.repository.IdentityVerificationRepository;
import com.example.kidhackbackend.users.efin.repository.PaymentPasswordHistoryRepository;
import com.example.kidhackbackend.users.efin.repository.PaymentPasswordRepository;
import com.example.kidhackbackend.users.filtering.repository.BlockRuleRepository;
import com.example.kidhackbackend.users.location.repository.PlaceEventRepository;
import com.example.kidhackbackend.users.location.repository.PlaceRepository;
import com.example.kidhackbackend.users.location.repository.TrackSegmentRepository;
import com.example.kidhackbackend.users.location.repository.TrackingSettingRepository;
import com.example.kidhackbackend.users.member.repository.RefreshTokenRepository;
import com.example.kidhackbackend.users.monitoring.repository.AppUsageRepository;
import com.example.kidhackbackend.users.pairing.repository.PairingCodeRepository;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.screenshot.repository.ScreenshotRepository;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.CardRepository;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 하나가 <b>{@code Long id}(ownerId·childId·memberId·parentId)로만 참조되는</b> 도메인 데이터를
 * 한 곳에서 정리한다.
 *
 * <p>{@link MemberRemover} 는 {@code Member} 를 <b>JPA FK 로 무는</b> 6종(커뮤니티·문의·공지·자기참조)만
 * 지운다 — 그 6종은 FK 제약 때문에 안 지우면 회원 행 삭제 자체가 막힌다. 반면 지갑·결제·위치·앱제어·
 * 채팅 등은 회원을 FK 로 물지 않고 {@code Long} id 만 들고 있어 회원 삭제를 막지는 않지만, 그대로 두면
 * <b>주인 없는 고아 행</b>으로 남는다. 재가입한 동명이인이 옛 자녀의 위치·결제 데이터를 물려받는 사고를
 * 막으려면 함께 지워야 한다.
 *
 * <p>정리 순서를 이 한 곳에 모으는 이유는 {@link MemberRemover}·{@code CommunityPostRemover} 와 같다 —
 * 지우는 경로가 늘어도 각자 정리하게 두면 한쪽만 고쳐져 다시 어긋난다.
 *
 * <p><b>회원은 부모(PARENT) 또는 자녀(CHILD) 한 역할</b>이라, 무관한 도메인의 벌크 삭제는 매칭되는 행이
 * 없어 자연히 0건이 지워진다(예: 부모 id 로 {@code deleteByChildId} 를 불러도 지워질 자녀 데이터가 없다).
 * 그래서 역할 분기 없이 모든 도메인을 한 번에 훑어도 안전하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberDomainDataRemover {

    // 지갑/결제
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final CardRepository cardRepository;
    private final ChargeOrderRepository chargeOrderRepository;
    private final QrPaymentRepository qrPaymentRepository;

    // 전자금융 인증
    private final PaymentPasswordRepository paymentPasswordRepository;
    private final PaymentPasswordHistoryRepository paymentPasswordHistoryRepository;
    private final IdentityVerificationRepository identityVerificationRepository;

    // 위치
    private final PlaceRepository placeRepository;
    private final PlaceEventRepository placeEventRepository;
    private final TrackSegmentRepository trackSegmentRepository;
    private final TrackingSettingRepository trackingSettingRepository;

    // 앱 제어/필터링/모니터링/기기/스크린샷
    private final AppControlRuleRepository appControlRuleRepository;
    private final InstalledAppRepository installedAppRepository;
    private final ScreenTimeSettingRepository screenTimeSettingRepository;
    private final BlockRuleRepository blockRuleRepository;
    private final AppUsageRepository appUsageRepository;
    private final DeviceEventRepository deviceEventRepository;
    private final ScreenshotRepository screenshotRepository;

    // 페어링/토큰
    private final PairingCodeRepository pairingCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // 채팅(Mongo)
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 회원 id 로 참조되는 모든 도메인 데이터를 지운다.
     *
     * <p>RDB 정리는 호출자({@link MemberRemover#remove})의 트랜잭션에 참여한다 — 하나라도 실패하면 함께
     * 롤백돼 회원이 안 지워지고 정리 배치가 다음 주기에 다시 시도한다. 채팅만은 별도 저장소(Mongo)라
     * 이 트랜잭션에 묶이지 않으므로, Mongo 장애가 회원 물리삭제(→ 재가입)를 막지 않도록 따로 감싼다.
     *
     * @param memberId 지워지는 회원 id
     */
    @Transactional
    public void removeAllFor(Long memberId) {
        // 지갑: 원장(walletId 참조)을 먼저 지운 뒤 지갑을 지운다.
        walletRepository.findByOwnerId(memberId)
                .map(Wallet::getId)
                .ifPresent(walletTransactionRepository::deleteByWalletId);
        walletRepository.deleteByOwnerId(memberId);
        cardRepository.deleteByOwnerId(memberId);
        chargeOrderRepository.deleteByOwnerId(memberId);
        qrPaymentRepository.deleteByChildId(memberId);

        // 전자금융 인증
        paymentPasswordRepository.deleteByMemberId(memberId);
        paymentPasswordHistoryRepository.deleteByMemberId(memberId);
        identityVerificationRepository.deleteByMemberId(memberId);

        // 위치
        placeEventRepository.deleteByChildId(memberId);
        placeRepository.deleteByChildId(memberId);
        trackSegmentRepository.deleteByChildId(memberId);
        trackingSettingRepository.deleteByChildId(memberId);

        // 앱 제어/필터링/모니터링/기기/스크린샷
        appControlRuleRepository.deleteByChildId(memberId);
        installedAppRepository.deleteByChildId(memberId);
        screenTimeSettingRepository.deleteByChildId(memberId);
        blockRuleRepository.deleteByChildId(memberId);
        appUsageRepository.deleteByChildId(memberId);
        deviceEventRepository.deleteByChildId(memberId);
        screenshotRepository.deleteByChildId(memberId);

        // 페어링 코드 · 리프레시 토큰
        pairingCodeRepository.deleteByParentId(memberId);
        refreshTokenRepository.deleteByMemberId(memberId);

        // 채팅(Mongo) — 실패해도 회원 물리삭제는 계속되게 격리한다.
        removeChat(memberId);
    }

    /**
     * 회원이 참여한 채팅방과 그 메시지를 지운다(Mongo).
     *
     * <p>방은 부모·자녀 둘을 참여자로 갖는다. {@code findByParentIdOrChildId(id, id)} 로 내가 부모든
     * 자녀든 속한 방을 모아, 방마다 메시지를 먼저 지우고 방을 지운다. Mongo 는 JPA 트랜잭션 밖이라
     * 여기서 예외가 나면 회원 물리삭제까지 막혀 재가입이 지연되므로, 경고만 남기고 넘어간다.
     */
    private void removeChat(Long memberId) {
        try {
            List<ChatRoom> rooms = chatRoomRepository.findByParentIdOrChildId(memberId, memberId);
            for (ChatRoom room : rooms) {
                chatMessageRepository.deleteByRoomId(room.getId());
            }
            chatRoomRepository.deleteAll(rooms);
        } catch (Exception e) {
            log.warn("회원 {} 채팅(Mongo) 정리 실패 — 물리삭제는 계속 진행한다: {}", memberId, e.toString());
        }
    }
}
