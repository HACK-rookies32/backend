package com.example.kidhackbackend.users.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.appcontrol.entity.InstalledApp;
import com.example.kidhackbackend.users.appcontrol.repository.InstalledAppRepository;
import com.example.kidhackbackend.users.efin.entity.PaymentPassword;
import com.example.kidhackbackend.users.efin.repository.PaymentPasswordRepository;
import com.example.kidhackbackend.users.location.entity.Place;
import com.example.kidhackbackend.users.location.entity.PlaceType;
import com.example.kidhackbackend.users.location.repository.PlaceRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.RefreshToken;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.member.repository.RefreshTokenRepository;
import com.example.kidhackbackend.users.pairing.entity.PairingCode;
import com.example.kidhackbackend.users.pairing.repository.PairingCodeRepository;
import com.example.kidhackbackend.users.wallet.entity.Card;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.repository.CardRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 회원 물리삭제 시, 회원을 {@code Long id} 로만 참조하던 도메인 데이터까지 함께 정리되는지 검증한다.
 *
 * <p>물리삭제의 실제 트리거는 {@code WithdrawnMemberCleanupScheduler} 이지만, 그 조회 쿼리
 * ({@code is_deleted=true})가 H2(MODE=Oracle)에서 "INTEGER vs BOOLEAN" 으로 실패하는 기존 이슈가 있어
 * 로컬/CI 에선 스케줄러가 돌지 않는다. 그래서 여기서는 {@link MemberRemover#remove}(스케줄러가 호출하는
 * 바로 그 메서드)를 직접 불러 정리 로직만 독립적으로 검증한다.
 *
 * <p>대표적으로 ownerId·memberId·childId·parentId·walletId 참조 패턴을 모두 한 번씩 시드해 둔다.
 */
@SpringBootTest
class MemberWithdrawalCleanupIntegrationTest {

    @Autowired MemberRemover memberRemover;
    @Autowired MemberRepository memberRepository;
    @Autowired WalletRepository walletRepository;
    @Autowired WalletTransactionRepository walletTransactionRepository;
    @Autowired CardRepository cardRepository;
    @Autowired PaymentPasswordRepository paymentPasswordRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired InstalledAppRepository installedAppRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PairingCodeRepository pairingCodeRepository;

    @Test
    void remove_deletes_all_long_id_referenced_domain_data() {
        // --- 부모/자녀 준비 ---
        Member parent = newMember("cleanup_parent", Role.PARENT, null);
        memberRepository.save(parent);
        Member child = newMember("cleanup_child", Role.CHILD, parent);
        memberRepository.save(child);
        Long childId = child.getId();

        // --- 자녀 id 로 참조되는 도메인 데이터 시드 ---
        Wallet wallet = walletRepository.save(new Wallet(childId));           // ownerId
        Long walletId = wallet.getId();
        walletTransactionRepository.save(
                new WalletTransaction(walletId, TxType.PAYMENT, 1000L, 0L, "ref-1")); // walletId
        cardRepository.save(new Card(childId, "1234123412341234", "12/28", "12", "140303", "ChildLee")); // ownerId
        paymentPasswordRepository.save(new PaymentPassword(childId, "hash"));  // memberId
        placeRepository.save(new Place(childId, "home", 37.5, 127.0, 100.0, PlaceType.GEOFENCE)); // childId
        installedAppRepository.save(new InstalledApp(childId, "com.foo.app", "Foo")); // childId
        refreshTokenRepository.save(new RefreshToken(childId, "tok-1", LocalDateTime.now().plusDays(1))); // memberId
        // parentId 참조 경로 검증용 — 값만 childId 로 넣어 deleteByParentId(childId) 가 도는지 본다.
        pairingCodeRepository.save(new PairingCode("CODE12", childId, LocalDateTime.now().plusMinutes(10)));

        // --- 시드가 실제로 들어갔는지 확인 ---
        assertThat(walletRepository.findByOwnerId(childId)).isPresent();
        assertThat(walletTransactionRepository.findByWalletIdRecentFirst(walletId)).isNotEmpty();
        assertThat(cardRepository.findByOwnerIdOrderByIdDesc(childId)).isNotEmpty();
        assertThat(paymentPasswordRepository.existsByMemberId(childId)).isTrue();
        assertThat(placeRepository.findByChildId(childId)).isNotEmpty();
        assertThat(installedAppRepository.findByChildId(childId)).isNotEmpty();
        assertThat(refreshTokenRepository.countByMemberId(childId)).isEqualTo(1L);
        assertThat(pairingCodeRepository.findByParentIdAndExpiresAtAfterOrderByExpiresAtDesc(
                childId, LocalDateTime.now())).isNotEmpty();

        // --- 물리삭제 실행(스케줄러가 부르는 그 메서드) ---
        memberRemover.remove(child);

        // --- 회원 행과 모든 도메인 데이터가 사라졌는지 확인 ---
        assertThat(memberRepository.findById(childId)).isEmpty();
        assertThat(walletRepository.findByOwnerId(childId)).isEmpty();
        assertThat(walletTransactionRepository.findByWalletIdRecentFirst(walletId)).isEmpty();
        assertThat(cardRepository.findByOwnerIdOrderByIdDesc(childId)).isEmpty();
        assertThat(paymentPasswordRepository.existsByMemberId(childId)).isFalse();
        assertThat(placeRepository.findByChildId(childId)).isEmpty();
        assertThat(installedAppRepository.findByChildId(childId)).isEmpty();
        assertThat(refreshTokenRepository.countByMemberId(childId)).isZero();
        assertThat(pairingCodeRepository.findByParentIdAndExpiresAtAfterOrderByExpiresAtDesc(
                childId, LocalDateTime.now())).isEmpty();
    }

    private Member newMember(String loginId, Role role, Member parent) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setUsername(loginId);
        m.setPassword("x");
        m.setNickname(loginId);
        m.setEmail(role == Role.CHILD ? null : loginId + "@test.com");
        m.setPhoneNumber("010-0000-0000");
        m.setBirthDate(LocalDate.of(2014, 3, 3));
        m.setRole(role);
        if (parent != null) {
            m.setParent(parent);
        }
        return m;
    }
}
