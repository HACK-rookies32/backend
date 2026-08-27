package com.example.kidhackbackend.adminfinance;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.admin.finance.dto.AdminTxPageResponse;
import com.example.kidhackbackend.admin.finance.dto.FinanceStatsResponse;
import com.example.kidhackbackend.admin.finance.service.AdminFinanceQueryService;
import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 관리자 자금 조회(No.47·49·51) 슬라이스 테스트 — 전체 내역 필터·페이징 / 회원 지갑 / 통계(DAY·MERCHANT).
 */
@DataJpaTest
@Import({AdminFinanceQueryService.class, JpaAuditingConfig.class})
class AdminFinanceQueryServiceTest {

    private static final long USER_A = 100L;
    private static final long USER_B = 200L;

    @Autowired AdminFinanceQueryService service;
    @Autowired WalletRepository walletRepository;
    @Autowired WalletTransactionRepository txRepository;
    @Autowired QrPaymentRepository qrRepository;

    private long wallet(long ownerId, long balance) {
        Wallet w = new Wallet(ownerId);
        w.credit(balance);
        return walletRepository.save(w).getId();
    }

    private void tx(long walletId, TxType type, long amount, long balanceAfter) {
        txRepository.save(new WalletTransaction(walletId, type, amount, balanceAfter, "ref"));
    }

    private void paidQr(long childId, String token, String store, long amount) {
        QrPayment q = new QrPayment(token, childId, LocalDateTime.now().plusSeconds(60));
        q.markPaid(amount, store, 1L, LocalDateTime.now());
        qrRepository.save(q);
    }

    @Test
    void 전체내역_필터없이_모두_조회() {
        long wa = wallet(USER_A, 5000L);
        long wb = wallet(USER_B, 3000L);
        tx(wa, TxType.CHARGE, 5000L, 5000L);
        tx(wa, TxType.PAYMENT, 2000L, 3000L);
        tx(wb, TxType.CHARGE, 3000L, 3000L);

        AdminTxPageResponse res = service.getTransactions(null, null, null, null, 0, 20);
        assertThat(res.totalElements()).isEqualTo(3);
    }

    @Test
    void 전체내역_userId_type_필터() {
        long wa = wallet(USER_A, 5000L);
        long wb = wallet(USER_B, 3000L);
        tx(wa, TxType.CHARGE, 5000L, 5000L);
        tx(wa, TxType.PAYMENT, 2000L, 3000L);
        tx(wb, TxType.CHARGE, 3000L, 3000L);

        assertThat(service.getTransactions(USER_A, null, null, null, 0, 20).totalElements()).isEqualTo(2);
        assertThat(service.getTransactions(null, TxType.PAYMENT, null, null, 0, 20).totalElements()).isEqualTo(1);
    }

    @Test
    void 없는_회원_userId면_빈_결과() {
        wallet(USER_A, 5000L);
        AdminTxPageResponse res = service.getTransactions(999L, null, null, null, 0, 20);
        assertThat(res.totalElements()).isZero();
    }

    @Test
    void userId지정시_잔액도_함께_반환() {
        long wa = wallet(USER_A, 5000L);
        tx(wa, TxType.CHARGE, 5000L, 5000L);
        tx(wa, TxType.PAYMENT, 2000L, 3000L);

        AdminTxPageResponse res = service.getTransactions(USER_A, null, null, null, 0, 20);
        assertThat(res.walletBalance()).isEqualTo(5000L);
        assertThat(res.content()).hasSize(2);
    }

    @Test
    void userId없이_전체조회시_잔액은_null() {
        wallet(USER_A, 5000L);
        AdminTxPageResponse res = service.getTransactions(null, null, null, null, 0, 20);
        assertThat(res.walletBalance()).isNull();
    }

    @Test
    void 통계_DAY_충전결제_합계() {
        long wa = wallet(USER_A, 5000L);
        long wb = wallet(USER_B, 3000L);
        tx(wa, TxType.CHARGE, 5000L, 5000L);
        tx(wa, TxType.PAYMENT, 2000L, 3000L);
        tx(wb, TxType.CHARGE, 3000L, 3000L);

        FinanceStatsResponse res = service.getStats("DAY", null, null);
        assertThat(res.groupBy()).isEqualTo("DAY");
        assertThat(res.rows()).hasSize(1); // 모두 오늘
        assertThat(res.rows().get(0).chargeSum()).isEqualTo(8000L);
        assertThat(res.rows().get(0).paymentSum()).isEqualTo(2000L);
        assertThat(res.rows().get(0).count()).isEqualTo(3L);
    }

    @Test
    void 통계_MERCHANT_매장별_결제합계() {
        paidQr(1L, "qr_a", "GS25", 3000L);
        paidQr(2L, "qr_b", "GS25", 1000L);
        paidQr(3L, "qr_c", "CU", 2000L);

        FinanceStatsResponse res = service.getStats("MERCHANT", null, null);
        assertThat(res.groupBy()).isEqualTo("MERCHANT");
        assertThat(res.rows()).hasSize(2); // GS25, CU
        assertThat(res.rows().stream().filter(r -> r.key().equals("GS25")).findFirst().orElseThrow().paymentSum())
                .isEqualTo(4000L);
    }
}
