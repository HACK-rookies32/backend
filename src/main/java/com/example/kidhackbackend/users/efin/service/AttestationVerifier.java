package com.example.kidhackbackend.users.efin.service;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 모바일 네이티브 무결성 증명(attestation) 검증 — {@link TxSignatureVerifier} 의 자매 클래스.
 *
 * <p>모바일 네이티브(.so)가 서버 발급 nonce 와 기기 보안상태(state)를 공유 비밀키로 HMAC-SHA256
 * 서명해 보낸다. 서버는 같은 payload({@code nonce|state})를 재서명해 일치하는지 본다.
 * 로컬에서 state 를 0 으로 위조해도 이 서명을 통과하려면 .so 에 박힌 비밀키가 필요하다
 * → 로컬 후킹만으론 결제 관문을 못 넘는다(다단계 우회가 서버까지 확장).
 *
 * <p>⚠️ 공유 비밀키가 클라이언트(.so)에 내장돼 있어 리버싱으로 추출 가능한 것은 의도된 약점이다.
 * 실제 앱은 Google Play Integrity(하드웨어 기반) 로 이 한계를 넘는다.
 */
@Component
public class AttestationVerifier {

    private final byte[] secret;

    public AttestationVerifier(
            @Value("${kidhack.attestation-secret:kidhack-attest-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** {@code nonce|state} payload 를 HMAC-SHA256 서명한 hex 문자열. */
    public String sign(String nonce, String state) {
        String payload = nonce + "|" + state;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("attestation 서명 계산 실패", e);
        }
    }

    /** 모바일이 보낸 mac 이 nonce·state 와 일치하는지. */
    public boolean verify(String nonce, String state, String mac) {
        return mac != null && sign(nonce, state).equalsIgnoreCase(mac.trim());
    }
}
