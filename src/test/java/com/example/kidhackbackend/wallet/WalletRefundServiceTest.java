package com.example.kidhackbackend.wallet;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * 충전 환불/내역(No.37) 슬라이스 테스트 — 정상 환불·내역 + 의도적 취약점(IDOR/중복·음수) 재현.
 */
@DataJpaTest
@Import({WalletChargeService.class, WalletRefundService.class, WalletHistoryService.class, MockPgClient.class})
class WalletRefundServiceTest {

    private static final long PARENT_ID = 100L;

    @Autowired WalletChargeService chargeService;
    @Autowired WalletRefundService refundService;
    @Autowired WalletHistoryService historyService;
    @Autowired WalletRepository walletRepository;
    @Autowired ChargeOrderRepository chargeOrderRepository;
    @Autowired WalletTransactionRepository txRepository;

    /** 5000 충전한 주문을 만들고 merchantUid 반환. */
    private String charged(long ownerId, long amount) {
        ChargePrepareResponse p = chargeService.prepare(ownerId, new ChargePrepareRequest(amount));
        chargeService.approve(ownerId, new ChargeApproveRequest(p.merchantUid(), amount, null));
        return p.merchantUid();
    }

    @Test
    void 정상_환불하면_잔액이_차감되고_주문이_취소되며_원장이_남는다() {
        String muid = charged(PARENT_ID, 5000L);

        RefundResponse res = refundService.refund(PARENT_ID, muid);

        assertThat(res.refundedAmount()).isEqualTo(5000L);
        assertThat(res.balance()).isEqualTo(0L);
        assertThat(res.transaction().type()).isEqualTo("REFUND");
        assertThat(chargeOrderRepository.findByMerchantUid(muid).orElseThrow().getStatus())
                .isEqualTo(ChargeStatus.CANCELLED);
    }

    @Test
    void 내역조회_원장은_충전과_환불을_최신순으로_반환한다() {
        String muid = charged(PARENT_ID, 5000L);
        refundService.refund(PARENT_ID, muid);

        var txs = historyService.getTransactions(PARENT_ID);
        assertThat(txs).hasSize(2);
        assertThat(txs.get(0).type()).isEqualTo("REFUND"); // 최신순
        assertThat(txs.get(1).type()).isEqualTo("CHARGE");

        var charges = historyService.getCharges(PARENT_ID);
        assertThat(charges).hasSize(1);
        assertThat(charges.get(0).status()).isEqualTo("CANCELLED");
    }

    @Test
    void VULN_WALLET_5_타인_충전을_소유권검증없이_환불할수있다() {
        long victimId = 200L;
        String victimMuid = charged(victimId, 5000L); // 피해자 잔액 5000

        // 공격자(100L)가 피해자 주문을 환불
        RefundResponse res = refundService.refund(PARENT_ID, victimMuid);

        assertThat(res.balance()).isEqualTo(0L);
        // 피해자 지갑이 차감됨
        assertThat(walletRepository.findByOwnerId(victimId).orElseThrow().getBalance()).isEqualTo(0L);
    }

    @Test
    void VULN_WALLET_6_이미_환불된_주문을_재환불하면_잔액이_음수가_된다() {
        String muid = charged(PARENT_ID, 5000L);

        refundService.refund(PARENT_ID, muid); // 0
        RefundResponse second = refundService.refund(PARENT_ID, muid); // 상태/잔액 검증 없어 재차감

        // 취약: 이중 환불로 잔액이 -5000
        assertThat(second.balance()).isEqualTo(-5000L);
    }
}
