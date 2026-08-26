package com.example.kidhackbackend.adminfinance;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.admin.finance.dto.AdminActionResponse;
import com.example.kidhackbackend.admin.finance.service.AdminFinanceCommandService;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.entity.QrStatus;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 관리자 자금 변경(No.48·50) 슬라이스 테스트 — 강제환불/잔액조정 정상 + 취약점(사유·한도 미검증, 이중 강제환불).
 * (VULN-ADMIN-1 권한경계는 시큐리티/컨트롤러 계층 → 라이브에서 검증)
 */
@DataJpaTest
@Import({AdminFinanceCommandService.class})
class AdminFinanceCommandServiceTest {

    private static final long CHILD_ID = 300L;
    private static final long USER_ID = 400L;

    @Autowired AdminFinanceCommandService service;
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
    void 강제환불_자녀잔액_원복되고_CANCELLED된다() {
        giveBalance(CHILD_ID, 7000L);
        QrPayment qr = paidQr(CHILD_ID, "qr_a", 3000L);

        AdminActionResponse res = service.forceRefund(qr.getQrToken());

        assertThat(res.balance()).isEqualTo(10_000L);
        assertThat(res.transaction().type()).isEqualTo("REFUND");
        assertThat(qrRepository.findByQrToken("qr_a").orElseThrow().getStatus()).isEqualTo(QrStatus.CANCELLED);
    }

    @Test
    void 잔액조정_증액과_감액() {
        AdminActionResponse plus = service.adjust(USER_ID, 5000L, "이벤트 보상");
        assertThat(plus.balance()).isEqualTo(5000L);
        assertThat(plus.transaction().type()).isEqualTo("ADJUST");

        AdminActionResponse minus = service.adjust(USER_ID, -2000L, "오적립 회수");
        assertThat(minus.balance()).isEqualTo(3000L);
    }

    @Test
    void VULN_ADMIN_2_사유없이_거대금액_조정이_통과된다() {
        AdminActionResponse res = service.adjust(USER_ID, 999_999_999L, "");

        // 취약: reason 공백·한도 검증이 없어 임의 금액 조정(자금 창출).
        assertThat(res.balance()).isEqualTo(999_999_999L);
    }

    @Test
    void VULN_ADMIN_3_이미환불된_결제를_재강제환불해_중복환급() {
        giveBalance(CHILD_ID, 7000L);
        QrPayment qr = paidQr(CHILD_ID, "qr_a", 3000L);

        service.forceRefund(qr.getQrToken());              // 1차 → 10000
        AdminActionResponse twice = service.forceRefund(qr.getQrToken()); // 2차(이중) → 13000

        // 취약: 상태 미검증으로 또 3000 환급.
        assertThat(twice.balance()).isEqualTo(13_000L);
    }
}
