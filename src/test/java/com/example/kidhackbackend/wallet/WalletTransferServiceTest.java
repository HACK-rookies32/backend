package com.example.kidhackbackend.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.wallet.dto.TransferResponse;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import com.example.kidhackbackend.users.wallet.service.WalletTransferService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 부모↔자녀 이체/회수(No.39·40) 슬라이스 테스트 — 정상 이체·회수(+알림·이름) + 남은 취약점(페어링 IDOR) 재현
 * + T-1(잔액 음수 차단)·T-5(자기 자신 차단) 검증.
 */
@DataJpaTest
@Import({WalletTransferService.class, WalletTransferServiceTest.TestConfig.class})
class WalletTransferServiceTest {

    /** 알림 발행을 가로채는 테스트용 NotificationPort. */
    static class RecordingPort implements NotificationPort {
        final List<NotificationType> published = new ArrayList<>();
        @Override
        public void publish(Long recipientId, NotificationType type, String message) {
            published.add(type);
        }
    }

    static class TestConfig {
        @Bean
        RecordingPort recordingPort() {
            return new RecordingPort();
        }
    }

    @Autowired WalletTransferService service;
    @Autowired WalletRepository walletRepository;
    @Autowired WalletTransactionRepository txRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired RecordingPort recordingPort;

    @BeforeEach
    void clearNotifications() {
        recordingPort.published.clear(); // 싱글턴 빈이라 테스트 간 공유됨
    }

    private Long persistMember(Role role, String name) {
        Member m = new Member();
        m.setLoginId(role + "_" + System.nanoTime());
        m.setUsername(name);
        m.setPassword("pw");
        m.setPhoneNumber("010-1234-5678");
        m.setBirthDate(LocalDate.of(2015, 3, 21));
        m.setRole(role);
        return memberRepository.save(m).getId();
    }

    private void giveBalance(long ownerId, long balance) {
        Wallet w = new Wallet(ownerId);
        w.credit(balance);
        walletRepository.save(w);
    }

    @Test
    void 용돈이체_부모잔액이_자녀로_이동하고_이름과_알림이_담긴다() {
        Long parentId = persistMember(Role.PARENT, "홍길동");
        Long childId = persistMember(Role.CHILD, "홍아들");
        giveBalance(parentId, 10_000L);

        TransferResponse res = service.transfer(parentId, childId, 3000L);

        assertThat(res.fromBalance()).isEqualTo(7000L);
        assertThat(res.toBalance()).isEqualTo(3000L);
        assertThat(res.fromOwnerName()).isEqualTo("홍길동");
        assertThat(res.toOwnerName()).isEqualTo("홍아들");
        assertThat(walletRepository.findByOwnerId(childId).orElseThrow().getBalance()).isEqualTo(3000L);
        assertThat(recordingPort.published).containsExactly(NotificationType.ALLOWANCE_RECEIVED);
    }

    @Test
    void 잔액회수_자녀잔액이_부모로_이동하고_회수_알림이_간다() {
        Long parentId = persistMember(Role.PARENT, "홍길동");
        Long childId = persistMember(Role.CHILD, "홍아들");
        giveBalance(childId, 5000L);

        TransferResponse res = service.recover(parentId, childId, 2000L);

        assertThat(res.fromOwnerId()).isEqualTo(childId);
        assertThat(res.toOwnerId()).isEqualTo(parentId);
        assertThat(walletRepository.findByOwnerId(childId).orElseThrow().getBalance()).isEqualTo(3000L);
        assertThat(walletRepository.findByOwnerId(parentId).orElseThrow().getBalance()).isEqualTo(2000L);
        assertThat(recordingPort.published).containsExactly(NotificationType.ALLOWANCE_RECLAIMED);
    }

    @Test
    void VULN_WALLET_7_페어링검증없이_남의_자녀에게_이체할수있다() {
        Long attackerParent = persistMember(Role.PARENT, "공격자");
        Long strangerChild = persistMember(Role.CHILD, "남의아이"); // attackerParent 의 자녀로 연결하지 않음
        giveBalance(attackerParent, 10_000L);

        TransferResponse res = service.transfer(attackerParent, strangerChild, 5000L);

        // 취약: 소유권(페어링) 검증이 없어 남의 자녀 지갑에 입금된다.
        assertThat(res.toBalance()).isEqualTo(5000L);
        assertThat(walletRepository.findByOwnerId(strangerChild).orElseThrow().getBalance()).isEqualTo(5000L);
    }

    @Test
    void T1_잔액초과_이체는_거부된다() {
        Long parentId = persistMember(Role.PARENT, "홍길동");
        Long childId = persistMember(Role.CHILD, "홍아들");
        giveBalance(parentId, 1000L);

        assertThatThrownBy(() -> service.transfer(parentId, childId, 5000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔액이 부족");

        assertThat(walletRepository.findByOwnerId(parentId).orElseThrow().getBalance()).isEqualTo(1000L);
    }

    @Test
    void T5_자기_자신에게는_이체할수없다() {
        Long parentId = persistMember(Role.PARENT, "홍길동");
        giveBalance(parentId, 10_000L);

        assertThatThrownBy(() -> service.transfer(parentId, parentId, 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자기 자신");
    }

    @Test
    void 존재하지_않는_자녀로는_이체할수없다() {
        Long parentId = persistMember(Role.PARENT, "홍길동");
        giveBalance(parentId, 10_000L);

        assertThatThrownBy(() -> service.transfer(parentId, 999_999L, 1000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자녀를 찾을 수 없습니다");
    }
}
