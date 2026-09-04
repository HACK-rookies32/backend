package com.example.kidhackbackend.global.crypto;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 모바일 네이티브 무결성 증명(attestation)용 challenge nonce 저장소.
 *
 * <p>challenge 엔드포인트가 {@link #issue()} 로 임의 nonce 를 발급해 기록하고(TTL),
 * 결제 검증이 {@link #consume(String)} 로 그 nonce 를 <b>한 번만</b> 소진한다.
 * 발급된 적 없거나 · 만료됐거나 · 이미 소진된 nonce 는 거부된다(재전송 방지).
 *
 * <p>challenge-response 의 핵심: 서버가 발급한 nonce 여야 하므로, 공격자가 미리 계산해 둔
 * 서명을 재사용할 수 없다. (단, .so 에 내장된 HMAC 키를 추출하면 위조 가능 — 의도된 약점)
 *
 * <p>기존 {@link NonceStore} 와 달리 "발급 사실"까지 추적하는 점이 다르다. H2 로컬이라
 * 인메모리 맵으로 관리한다(유효창이 짧아 부담 없음).
 */
@Component
public class AttestationNonceStore {

    private final long ttlMillis;
    private final SecureRandom random = new SecureRandom();
    /** nonce -> 만료 시각(epoch millis). */
    private final Map<String, Long> issued = new ConcurrentHashMap<>();

    public AttestationNonceStore(
            @Value("${kidhack.card.nonce-ttl-seconds:60}") long ttlSeconds) {
        this.ttlMillis = ttlSeconds * 1000L;
    }

    /** 임의 nonce(128비트 hex)를 발급해 기록하고 반환한다. */
    public synchronized String issue() {
        long now = System.currentTimeMillis();
        evictExpired(now);
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String nonce = HexFormat.of().formatHex(bytes);
        issued.put(nonce, now + ttlMillis);
        return nonce;
    }

    /**
     * nonce 를 소진한다. 발급된 적 있고 · 만료 전이며 · 처음 소비되는 경우에만 true.
     * (성공/실패와 무관하게 제거해 재사용을 막는다.)
     */
    public synchronized boolean consume(String nonce) {
        long now = System.currentTimeMillis();
        evictExpired(now);
        Long expiry = issued.remove(nonce);
        return expiry != null && expiry > now;
    }

    private void evictExpired(long now) {
        issued.entrySet().removeIf(e -> e.getValue() <= now);
    }
}
