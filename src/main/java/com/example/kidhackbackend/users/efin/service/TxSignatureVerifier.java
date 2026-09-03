package com.example.kidhackbackend.users.efin.service;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 거래정보 전자서명(무결성) 검증 — 금액·거래정보 변조 방지.
 *
 * <p>클라이언트가 거래 요청을 보낼 때 {@code amount:childId:nonce} 를 공유 비밀키로 HMAC-SHA256 서명해
 * 함께 보낸다. 서버는 같은 payload 를 재서명해 일치하는지 본다 — 중간에서 금액을 바꾸면 서명이 깨진다.
 *
 * <p>⚠️ 서명 검증의 "존재"만 담보하고, 검증을 <b>건너뛰는 우회로</b>는 상위 {@code EfinTxGuard} 에 있다
 * ([VULN-EFIN-2] 서명 필드가 없으면 검증 skip).
 */
@Component
public class TxSignatureVerifier {

    private final byte[] secret;

    public TxSignatureVerifier(
            @Value("${kidhack.efin.tx-signing-secret:kidhack-efin-tx-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** {@code amount:childId:nonce} payload 를 HMAC-SHA256 서명한 hex 문자열을 돌려준다. */
    public String sign(long amount, long refId, String nonce) {
        String payload = amount + ":" + refId + ":" + (nonce == null ? "" : nonce);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("서명 계산 실패", e);
        }
    }

    /** 서명이 payload 와 일치하는지. */
    public boolean verify(long amount, long refId, String nonce, String signature) {
        return signature != null && sign(amount, refId, nonce).equalsIgnoreCase(signature.trim());
    }
}