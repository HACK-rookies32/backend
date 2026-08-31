package com.example.kidhackbackend.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.wallet.dto.ChargeApproveRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargeApproveResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareResponse;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
import com.example.kidhackbackend.users.wallet.pg.MockPgClient;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import com.example.kidhackbackend.users.wallet.service.WalletChargeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 지갑 충전 서비스 슬라이스 테스트 — 정상 플로우 + 의도적 취약점(모의해킹) 재현.
 */
@DataJpaTest
@Import({WalletChargeService.class, MockPgClient.class})
class WalletChargeServiceTest {

    private static final long PARENT_ID = 100L;

    @Autowired WalletChargeService service;
    @Autowired WalletRepository walletRepository;
    @Autowired ChargeOrderRepository chargeOrderRepository;
    @Autowired WalletTransactionRepository txRepository;

    @Test
    void 준비하면_READY주문과_지갑이_생성된다() {
        ChargePrepareResponse res = service.prepare(PARENT_ID, new ChargePrepareRequest(5000L));

        assertThat(res.merchantUid()).startsWith("kid_");
        assertThat(res.amount()).isEqualTo(5000L);
        assertThat(chargeOrderRepository.findByMerchantUid(res.merchantUid()))
                .get()
                .extracting(o -> o.getStatus())
                .isEqualTo(ChargeStatus.READY);
        assertThat(walletRepository.findByOwnerId(PARENT_ID)).isPresent();
    }

    @Test
    void 정상_승인하면_잔액이_적립되고_원장이_기록된다() {
        ChargePrepareResponse prepared = service.prepare(PARENT_ID, new ChargePrepareRequest(5000L));

        ChargeApproveResponse res = service.approve(
                PARENT_ID, new ChargeApproveRequest(prepared.merchantUid(), 5000L, null));

        assertThat(res.balance()).isEqualTo(5000L);
        assertThat(res.transaction().amount()).isEqualTo(5000L);
        Long walletId = walletRepository.findByOwnerId(PARENT_ID).orElseThrow().getId();
        assertThat(txRepository.findByWalletIdRecentFirst(walletId)).hasSize(1);
        assertThat(chargeOrderRepository.findByMerchantUid(prepared.merchantUid()).orElseThrow()
                .getStatus()).isEqualTo(ChargeStatus.PAID);
    }

    @Test
    void VULN_WALLET_1_금액변조_준비금액과다르게_승인해도_그대로_적립된다() {
        // 100원짜리 주문을 준비하고, 승인 시 100만원으로 변조해 보낸다.
        ChargePrepareResponse prepared = service.prepare(PARENT_ID, new ChargePrepareRequest(100L));

        ChargeApproveResponse res = service.approve(
                PARENT_ID, new ChargeApproveRequest(prepared.merchantUid(), 1_000_000L, null));

        // 취약: 준비 금액(100)과 대조 없이 클라이언트가 보낸 100만원이 적립된다.
        assertThat(res.balance()).isEqualTo(1_000_000L);
    }

    @Test
    void VULN_WALLET_3_동일주문_반복승인시_잔액이_중복적립된다() {
        ChargePrepareResponse prepared = service.prepare(PARENT_ID, new ChargePrepareRequest(5000L));
        ChargeApproveRequest approve =
                new ChargeApproveRequest(prepared.merchantUid(), 5000L, null);

        service.approve(PARENT_ID, approve);
        ChargeApproveResponse second = service.approve(PARENT_ID, approve);

        // 취약: 멱등성 가드가 없어 두 번 승인하면 1만원이 된다.
        assertThat(second.balance()).isEqualTo(10_000L);
    }

    @Test
    void VULN_WALLET_2_타인주문을_소유권검증없이_승인할수있다() {
        // 피해자(200L)가 준비한 주문을, 공격자(PARENT_ID=100L)가 승인한다.
        long victimId = 200L;
        ChargePrepareResponse victimOrder = service.prepare(victimId, new ChargePrepareRequest(3000L));

        ChargeApproveResponse res = service.approve(
                PARENT_ID, new ChargeApproveRequest(victimOrder.merchantUid(), 3000L, null));

        // 취약: 요청자(100L)와 주문 소유자(200L)가 달라도 승인되고, 잔액은 피해자 지갑으로 들어간다.
        assertThat(res.balance()).isEqualTo(3000L);
        assertThat(walletRepository.findByOwnerId(victimId).orElseThrow().getBalance())
                .isEqualTo(3000L);
    }
}
