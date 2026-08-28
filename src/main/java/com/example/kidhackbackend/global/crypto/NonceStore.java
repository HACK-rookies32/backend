package com.example.kidhackbackend.global.crypto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 재전송(replay) 방지용 nonce 저장소(인메모리, TTL).
 *
 * <p>규약 확정본: nonce+timestamp를 암호문 내부에 넣고, 서버는 timestamp가 유효창(기본 60초)
 * 이내인지 + nonce가 그 창에서 재사용되지 않았는지 검사한다. H2 프로젝트라 별도 테이블을 두지 않고
 * 메모리 맵으로 관리한다(유효창이 짧아 메모리 부담이 없다).
 *
 * <p>{@link #tryConsume}는 시간 기준값(now)을 파라미터로 받는다 — 테스트에서 시계를 고정하기 위함이며,
 * 컨트롤러/서비스가 {@code System.currentTimeMillis()}를 넘긴다.
 */
@Component
public class NonceStore {

    private final long ttlMillis;
    /** nonce -> 만료 시각(epoch millis). */
    private final Map<String, Long> seen = new ConcurrentHashMap<>();

    public NonceStore(@Value("${kidhack.card.nonce-ttl-seconds:60}") long ttlSeconds) {
        this.ttlMillis = ttlSeconds * 1000L;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    /**
     * nonce를 소비한다. 처음 보는 nonce면 기록하고 true, 이미 쓰였으면 false.
     * 호출 시 만료된 항목을 정리한다.
     *
     * @param nonce 요청 nonce
     * @param nowMillis 현재 시각(epoch millis)
     */
    public synchronized boolean tryConsume(String nonce, long nowMillis) {
        evictExpired(nowMillis);
        if (seen.containsKey(nonce)) {
            return false;
        }
        seen.put(nonce, nowMillis + ttlMillis);
        return true;
    }

    private void evictExpired(long nowMillis) {
        seen.entrySet().removeIf(e -> e.getValue() <= nowMillis);
    }
}
