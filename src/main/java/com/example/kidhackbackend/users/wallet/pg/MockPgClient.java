package com.example.kidhackbackend.users.wallet.pg;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PG 모의 구현. 실제 결제망 대신 항상 승인 성공을 반환하고, 웹훅 서명(HMAC-SHA256)을 계산/검증한다.
 *
 * <p><b>주의(모의해킹):</b> 이 Mock은 요청받은 {@code amount}를 그대로 "결제됨"으로 에코한다.
 * 실제 안전한 연동이라면 서버가 (1) 준비된 주문의 금액과 (2) PG가 보고한 실제 결제 금액을
 * 서버측에서 교차 검증해야 하지만, 본 실습에서는 승인 서비스가 그 교차 검증을 생략한다
 * (see {@code WalletChargeService} [VULN-WALLET-1]).
 *
 * <p>서명 자체({@link #sign}/{@link #verify})는 올바르게 구현한다. 취약점은 이 검증을
 * '건너뛰는' 웹훅 처리 쪽에 있다 (see {@code WalletChargeService.confirmByWebhook} [VULN-WALLET-4]).
 */
@Component
public class MockPgClient implements PgClient {

    /** 웹훅 서명용 공유 시크릿. application.yml 미수정 규칙 → 기본값 제공. */
    private final String webhookSecret;

    public MockPgClient(
            @Value("${kidhack.pg.webhook-secret:local-pg-webhook-secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    public PgCheckout checkout(String merchantUid, long amount) {
        // 모의 결제 — 실제 결제창이 없으므로 프론트는 곧바로 approve 를 호출한다.
        return new PgCheckout("MOCK", null, true);
    }

    @Override
    public PgApproval approve(String merchantUid, long amount) {
        String pgTransactionId = "imp_" + UUID.randomUUID().toString().replace("-", "");
        return new PgApproval(pgTransactionId, amount, "paid");
    }

    @Override
    public String sign(String rawBody) {
        return hmacSha256Hex(rawBody, webhookSecret);
    }

    @Override
    public boolean verify(String rawBody, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        return sign(rawBody).equalsIgnoreCase(signature);
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("웹훅 서명 계산 실패", e);
        }
    }
}
