package com.example.kidhackbackend.global.notification.push;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 실제로 보내지 않고 로그만 남긴다. 로컬/CI 기본값이며, 운영에서 Firebase 초기화가
 * 실패했을 때의 폴백이기도 하다(운영은 그 전에 기동이 막힌다 — {@link PushConfig} 참고).
 *
 * <p>빈 등록은 {@link PushConfig} 가 한다. 예전에는 이 클래스에
 * {@code @ConditionalOnProperty(matchIfMissing = true)} 가 붙어 있었는데, 슬라이스
 * 테스트에서는 그 경로를 타지 못해 컨텍스트가 뜨지 않는 문제가 있었다.
 */
@Slf4j
public class MockPushSender implements PushPort {

    @Override
    public PushResult sendPush(String deviceToken, PushMessage message) {
        log.info("[MOCK PUSH] token={}, title={}, body={}, data={}",
                deviceToken, message.title(), message.body(), message.data());
        return PushResult.SUCCESS;
    }

    @Override
    public Map<String, PushResult> sendMulticast(List<String> deviceTokens, PushMessage message) {
        log.info("[MOCK PUSH] 멀티캐스트 {} 건, title={}, body={}, data={}",
                deviceTokens.size(), message.title(), message.body(), message.data());
        Map<String, PushResult> results = new LinkedHashMap<>();
        deviceTokens.forEach(token -> results.put(token, PushResult.SUCCESS));
        return results;
    }
}