package com.example.kidhackbackend.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.users.wallet.dto.ChargePrepareRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareResponse;
import com.example.kidhackbackend.users.wallet.dto.PgWebhookRequest;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
import com.example.kidhackbackend.users.wallet.pg.MockPgClient;
import com.example.kidhackbackend.users.wallet.pg.PgClient;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import com.example.kidhackbackend.users.wallet.service.WalletChargeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * PG 웹훅 확정(No.36) 슬라이스 테스트 — 정상 서명 확정 + 의도적 취약점(서명 우회/재생) 재현.
 */
@DataJpaTest
@Import({WalletChargeService.class, MockPgClient.class})
class PgWebhookServiceTest {

    private static final long PARENT_ID = 100L;

    @Autowired WalletChargeService service;
    @Autowired PgClient pgClient;
    @Autowired WalletRepository walletRepository;
    @Autowired ChargeOrderRepository chargeOrderRepository;
    @Autowired WalletTransactionRepository txRepository;

    private String body(String merchantUid, String status, long paidAmount) {
        return "{\"merchantUid\":\"" + merchantUid + "\",\"status\":\"" + status
                + "\",\"paidAmount\":" + paidAmount + ",\"pgTransactionId\":\"imp_test\"}";
    }

    @Test
    void 유효한_서명이면_주문이_확정되고_잔액이_적립된다() {
        ChargePrepareResponse prepared = service.prepare(PARENT_ID, new ChargePrepareRequest(5000L));
        String raw = body(prepared.merchantUid(), "paid", 5000L);
        String sig = pgClient.sign(raw);

        service.confirmByWebhook(raw, sig, new PgWebhookRequest(prepared.merchantUid(), "paid", 5000L, "imp_test"));

        assertThat(walletRepository.findByOwnerId(PARENT_ID).orElseThrow().getBalance()).isEqualTo(5000L);
        assertThat(chargeOrderRepository.findByMerchantUid(prepared.merchantUid()).orElseThrow()
                .getStatus()).isEqualTo(ChargeStatus.PAID);
    }

    @Test
    void 서명이_위조되면_거부된다() {
        ChargePrepareResponse prepared = service.prepare(PARENT_ID, new ChargePrepareRequest(5000L));
        String raw = body(prepared.merchantUid(), "paid", 5000L);

        assertThatThrownBy(() -> service.confirmByWebhook(raw, "deadbeef",
                new PgWebhookRequest(prepared.merchantUid(), "paid", 5000L, "imp_test")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(walletRepository.findByOwnerId(PARENT_ID).orElseThrow().getBalance()).isEqualTo(0L);
    }

    @Test
    void VULN_WALLET_4_서명헤더가_없으면_검증을_건너뛰어_위조웹훅이_통과된다() {
        ChargePrepareResponse prepared = service.prepare(PARENT_ID, new ChargePrepareRequest(100L));
        // 서명(null) 없이, 금액을 100만원으로 위조한 웹훅.
        String raw = body(prepared.merchantUid(), "paid", 1_000_000L);

        service.confirmByWebhook(raw, null,
                new PgWebhookRequest(prepared.merchantUid(), "paid", 1_000_000L, "imp_forged"));

        // 취약: 서명 없이도 확정되어 100만원이 적립된다.
        assertThat(walletRepository.findByOwnerId(PARENT_ID).orElseThrow().getBalance()).isEqualTo(1_000_000L);
    }

    @Test
    void W2_동일_웹훅_재전송해도_중복_적립되지_않는다() {
        ChargePrepareResponse prepared = service.prepare(PARENT_ID, new ChargePrepareRequest(5000L));
        String raw = body(prepared.merchantUid(), "paid", 5000L);
        String sig = pgClient.sign(raw);
        PgWebhookRequest payload = new PgWebhookRequest(prepared.merchantUid(), "paid", 5000L, "imp_test");

        service.confirmByWebhook(raw, sig, payload);
        service.confirmByWebhook(raw, sig, payload); // 재전송

        // W-2: 멱등 가드로 두 번째는 무시된다.
        assertThat(walletRepository.findByOwnerId(PARENT_ID).orElseThrow().getBalance()).isEqualTo(5000L);
        assertThat(chargeOrderRepository.findByMerchantUid(prepared.merchantUid()).orElseThrow()
                .getStatus()).isEqualTo(ChargeStatus.PAID);
    }

    @Test
    void W1_결제실패_웹훅이면_READY주문이_FAILED로_전이된다() {
        ChargePrepareResponse prepared = service.prepare(PARENT_ID, new ChargePrepareRequest(5000L));
        String raw = body(prepared.merchantUid(), "cancelled", 5000L);
        String sig = pgClient.sign(raw);

        service.confirmByWebhook(raw, sig, new PgWebhookRequest(prepared.merchantUid(), "cancelled", 5000L, "imp_test"));

        assertThat(walletRepository.findByOwnerId(PARENT_ID).orElseThrow().getBalance()).isEqualTo(0L);
        assertThat(chargeOrderRepository.findByMerchantUid(prepared.merchantUid()).orElseThrow()
                .getStatus()).isEqualTo(ChargeStatus.FAILED);
    }

    @Test
    void paid도_실패도_아닌_status는_무시되고_주문이_READY로_남는다() {
        ChargePrepareResponse prepared = service.prepare(PARENT_ID, new ChargePrepareRequest(5000L));
        String raw = body(prepared.merchantUid(), "pending", 5000L);
        String sig = pgClient.sign(raw);

        service.confirmByWebhook(raw, sig, new PgWebhookRequest(prepared.merchantUid(), "pending", 5000L, "imp_test"));

        assertThat(walletRepository.findByOwnerId(PARENT_ID).orElseThrow().getBalance()).isEqualTo(0L);
        assertThat(chargeOrderRepository.findByMerchantUid(prepared.merchantUid()).orElseThrow()
                .getStatus()).isEqualTo(ChargeStatus.READY);
    }
}
