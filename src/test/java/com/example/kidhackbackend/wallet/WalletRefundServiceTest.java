package com.example.kidhackbackend.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.users.wallet.dto.ChargePrepareResponse;
import com.example.kidhackbackend.users.wallet.dto.RefundResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargeApproveRequest;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
import com.example.kidhackbackend.users.wallet.pg.MockPgClient;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import com.example.kidhackbackend.users.wallet.service.WalletChargeService;
import com.example.kidhackbackend.users.wallet.service.WalletHistoryService;
import com.example.kidhackbackend.users.wallet.service.WalletRefundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/**
 * 충전 환불/내역(No.37) 슬라이스 테스트 — 정상 환불·내역 + 남은 취약점(IDOR) 재현 + R-2(음수 방지) 검증.
 */
@DataJpaTest
@Import({WalletChargeService.class, WalletRefundService.class, WalletHistoryService.class, MockPgClient.class})
class WalletRefundServiceTest {

    private static final long PARENT_ID = 100L;
    private static final PageRequest PAGE = PageRequest.of(0, 20);

    @Autowired WalletChargeService chargeService;
    @Autowired WalletRefundService refundService;
    @Autowired WalletHistoryService historyService;
    @Autowired WalletRepository walletRepository;
    @Autowired ChargeOrderRepository chargeOrderRepository;
    @Autowired WalletTransactionRepository txRepository;

    /** amount 충전한 주문을 만들고 merchantUid 반환. */
    private String charged(long ownerId, long amount) {
        ChargePrepareResponse p = chargeService.prepare(ownerId, new ChargePrepareRequest(amount));
        chargeService.approve(ownerId, new ChargeApproveRequest(p.merchantUid(), amount, null));
        return p.merchantUid();
    }

    @Test
    void 정상_환불하면_잔액이_차감되고_주문이_REFUNDED_되며_원장이_남는다() {
        String muid = charged(PARENT_ID, 5000L);

        RefundResponse res = refundService.refund(PARENT_ID, muid);

        assertThat(res.refundedAmount()).isEqualTo(5000L);
        assertThat(res.balance()).isEqualTo(0L);
        assertThat(res.transaction().type()).isEqualTo("REFUND");
        assertThat(chargeOrderRepository.findByMerchantUid(muid).orElseThrow().getStatus())
                .isEqualTo(ChargeStatus.REFUNDED);
    }

    @Test
    void 내역조회_원장은_충전과_환불을_최신순으로_반환한다() {
        String muid = charged(PARENT_ID, 5000L);
        refundService.refund(PARENT_ID, muid);

        var txs = historyService.getTransactions(PARENT_ID, PAGE).content();
        assertThat(txs).hasSize(2);
        assertThat(txs.get(0).type()).isEqualTo("REFUND"); // 최신순
        assertThat(txs.get(1).type()).isEqualTo("CHARGE");

        var charges = historyService.getCharges(PARENT_ID, PAGE).content();
        assertThat(charges).hasSize(1);
        assertThat(charges.get(0).status()).isEqualTo("REFUNDED");
    }

    @Test
    void 통합내역은_환불시각을_함께_내려준다() {
        String muid = charged(PARENT_ID, 5000L);
        refundService.refund(PARENT_ID, muid);

        var history = historyService.getChargeHistory(PARENT_ID, PAGE).content();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("REFUNDED");
        assertThat(history.get(0).refundedAt()).isNotNull();
    }

    @Test
    void VULN_WALLET_5_타인_충전을_소유권검증없이_환불할수있다() {
        long victimId = 200L;
        String victimMuid = charged(victimId, 5000L); // 피해자 잔액 5000

        // 공격자(100L)가 피해자 주문을 환불 — 소유권 검증이 없어 통과
        RefundResponse res = refundService.refund(PARENT_ID, victimMuid);

        assertThat(res.balance()).isEqualTo(0L);
        // 피해자 지갑이 차감됨
        assertThat(walletRepository.findByOwnerId(victimId).orElseThrow().getBalance()).isEqualTo(0L);
    }

    @Test
    void R2_이미_환불된_주문_재환불은_잔액부족으로_거부된다() {
        String muid = charged(PARENT_ID, 5000L);

        refundService.refund(PARENT_ID, muid); // 잔액 0

        // R-2: 두 번째 환불은 잔액(0) < 환불액(5000) 이라 거부 → 음수 안 됨
        assertThatThrownBy(() -> refundService.refund(PARENT_ID, muid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔액이 부족");

        assertThat(walletRepository.findByOwnerId(PARENT_ID).orElseThrow().getBalance()).isEqualTo(0L);
    }
}
