package com.example.kidhackbackend.qrpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.qrpay.dto.QrCancelResponse;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.entity.QrStatus;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.qrpay.service.QrCancelService;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 매장결제 취소(No.46) 슬라이스 테스트 — 정상 취소/환급 + 취약점(소유권 IDOR·이중취소) 재현.
 */
@DataJpaTest
@Import({QrCancelService.class, QrCancelServiceTest.NoOpNotiConfig.class})
class QrCancelServiceTest {

    private static final long CHILD_ID = 300L;
    private static final long PARENT_ID = 500L;
    private static final long ATTACKER_PARENT_ID = 999L;

    @TestConfiguration
    static class NoOpNotiConfig {
        @Bean
        NotificationPort notificationPort() {
            return (recipientId, type, message) -> { /* no-op */ };
        }
    }

    @Autowired QrCancelService service;
    @Autowired QrPaymentRepository qrRepository;
    @Autowired WalletRepository walletRepository;

    private void giveBalance(long ownerId, long balance) {
        Wallet w = new Wallet(ownerId);
        w.credit(balance);
        walletRepository.save(w);
    }

    private QrPayment paidQr(long childId, String token, long amount) {
        QrPayment q = new QrPayment(token, childId, LocalDateTime.now().plusSeconds(60));
        q.markPaid(amount, "GS25", 1L, LocalDateTime.now());
        return qrRepository.save(q);
    }

    @Test
    void 정상취소_자녀잔액이_환급되고_QR이_CANCELLED된다() {
        giveBalance(CHILD_ID, 7000L);          // 결제 3000 후 남은 잔액이라 가정
        QrPayment qr = paidQr(CHILD_ID, "qr_a", 3000L);

        QrCancelResponse res = service.cancel(PARENT_ID, qr.getQrToken());

        assertThat(res.status()).isEqualTo("CANCELLED");
        assertThat(res.canceledAmount()).isEqualTo(3000L);
        assertThat(res.balance()).isEqualTo(10_000L);   // 7000 + 3000 환급
        assertThat(res.transaction().type()).isEqualTo("PAYMENT_CANCEL");
        assertThat(qrRepository.findByQrToken("qr_a").orElseThrow().getStatus()).isEqualTo(QrStatus.CANCELLED);
    }

    @Test
    void 없는_결제는_404() {
        assertThatThrownBy(() -> service.cancel(PARENT_ID, "qr_none"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void VULN_WALLET_9_소유권검증없이_남의_자녀_결제를_취소할수있다() {
        giveBalance(CHILD_ID, 7000L);
        QrPayment qr = paidQr(CHILD_ID, "qr_a", 3000L);

        // 공격자 부모(자녀의 부모 아님)가 qrToken만으로 취소
        QrCancelResponse res = service.cancel(ATTACKER_PARENT_ID, qr.getQrToken());

        // 취약: 환급은 결제한 자녀(CHILD_ID) 지갑으로 들어간다(공격자가 타 자녀 지갑 조작).
        assertThat(res.balance()).isEqualTo(10_000L);
        assertThat(walletRepository.findByOwnerId(CHILD_ID).orElseThrow().getBalance()).isEqualTo(10_000L);
    }

    @Test
    void VULN_WALLET_10_이미취소된_결제를_재취소해_잔액이_중복환급된다() {
        giveBalance(CHILD_ID, 7000L);
        QrPayment qr = paidQr(CHILD_ID, "qr_a", 3000L);

        service.cancel(PARENT_ID, qr.getQrToken());               // 1차 → 10000
        QrCancelResponse twice = service.cancel(PARENT_ID, qr.getQrToken()); // 2차(이중) → 13000

        // 취약: 상태 검증이 없어 또 3000이 환급된다(자금 창출).
        assertThat(twice.balance()).isEqualTo(13_000L);
    }
}
