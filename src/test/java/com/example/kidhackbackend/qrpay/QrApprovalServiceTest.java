package com.example.kidhackbackend.qrpay;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.qrpay.dto.QrApproveResponse;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.entity.QrStatus;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.qrpay.service.QrApprovalService;
import com.example.kidhackbackend.users.qrpay.service.StoreTerminalService;
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
 * QR 스캔 결제 승인(No.44) 슬라이스 테스트 — 정상 승인 + 취약점 4종(단말우회·재사용·만료·잔액) 재현.
 */
@DataJpaTest
@Import({QrApprovalService.class, StoreTerminalService.class, QrApprovalServiceTest.NoOpNotiConfig.class})
class QrApprovalServiceTest {

    private static final long CHILD_ID = 300L;

    @TestConfiguration
    static class NoOpNotiConfig {
        @Bean
        NotificationPort notificationPort() {
            return (recipientId, type, message) -> { /* no-op */ };
        }
    }

    @Autowired QrApprovalService service;
    @Autowired StoreTerminalService terminalService;
    @Autowired QrPaymentRepository qrRepository;
    @Autowired WalletRepository walletRepository;

    private String issueTerminal() {
        return terminalService.issue("GS25").terminalToken();
    }

    private void giveBalance(long ownerId, long balance) {
        Wallet w = new Wallet(ownerId);
        w.credit(balance);
        walletRepository.save(w);
    }

    private QrPayment issueQr(long childId, LocalDateTime expiresAt) {
        return qrRepository.save(new QrPayment("qr_" + childId + "_t", childId, expiresAt));
    }

    @Test
    void 정상승인_자녀잔액이_차감되고_QR이_PAID된다() {
        giveBalance(CHILD_ID, 10_000L);
        String term = issueTerminal();
        QrPayment qr = issueQr(CHILD_ID, LocalDateTime.now().plusSeconds(60));

        QrApproveResponse res = service.approve(term, qr.getQrToken(), 3000L);

        assertThat(res.status()).isEqualTo("PAID");
        assertThat(res.balance()).isEqualTo(7000L);
        assertThat(res.storeName()).isEqualTo("GS25");
        assertThat(res.transaction().type()).isEqualTo("PAYMENT");
        assertThat(qrRepository.findByQrToken(qr.getQrToken()).orElseThrow().getStatus())
                .isEqualTo(QrStatus.PAID);
    }

    @Test
    void VULN_WALLET_15_단말토큰_없이도_결제가_승인된다() {
        giveBalance(CHILD_ID, 10_000L);
        QrPayment qr = issueQr(CHILD_ID, LocalDateTime.now().plusSeconds(60));

        // 헤더(단말 토큰) 없이(null) 호출 → 검증 스킵으로 승인됨
        QrApproveResponse res = service.approve(null, qr.getQrToken(), 3000L);

        assertThat(res.status()).isEqualTo("PAID");
        assertThat(res.storeName()).isEqualTo("UNKNOWN"); // 단말 미인증
        assertThat(res.balance()).isEqualTo(7000L);
    }

    @Test
    void VULN_WALLET_16_같은_QR을_재사용해_중복_결제된다() {
        giveBalance(CHILD_ID, 10_000L);
        String term = issueTerminal();
        QrPayment qr = issueQr(CHILD_ID, LocalDateTime.now().plusSeconds(60));

        service.approve(term, qr.getQrToken(), 3000L); // 1회차 → 7000
        QrApproveResponse res = service.approve(term, qr.getQrToken(), 3000L); // 2회차(재사용) → 4000

        assertThat(res.balance()).isEqualTo(4000L); // 상태 미검증으로 중복 차감
    }

    @Test
    void VULN_WALLET_17_만료된_QR도_승인된다() {
        giveBalance(CHILD_ID, 10_000L);
        String term = issueTerminal();
        QrPayment qr = issueQr(CHILD_ID, LocalDateTime.now().minusSeconds(1)); // 이미 만료

        QrApproveResponse res = service.approve(term, qr.getQrToken(), 3000L);

        assertThat(res.status()).isEqualTo("PAID"); // 만료 미검증으로 승인됨
    }

    @Test
    void VULN_WALLET_18_잔액초과_결제로_잔액이_음수가_된다() {
        giveBalance(CHILD_ID, 1000L);
        String term = issueTerminal();
        QrPayment qr = issueQr(CHILD_ID, LocalDateTime.now().plusSeconds(60));

        QrApproveResponse res = service.approve(term, qr.getQrToken(), 5000L);

        assertThat(res.balance()).isEqualTo(-4000L); // 잔액 미검증으로 음수
    }
}
