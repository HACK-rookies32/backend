package com.example.kidhackbackend.global.notification;

import com.example.kidhackbackend.global.notification.push.PushMessage;
import com.example.kidhackbackend.global.notification.push.PushPort;
import com.example.kidhackbackend.global.notification.push.PushResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 {@link PushPort}. 무엇을 어떤 내용으로 보냈는지 기록하고, 토큰별 결과를
 * 원하는 값으로 지정할 수 있다(무효 토큰 정리 검증용).
 */
public class FakePushSender implements PushPort {

    public record Sent(String token, PushMessage message) {
    }

    private final List<Sent> sent = new ArrayList<>();
    private final Map<String, PushResult> results = new HashMap<>();

    @Override
    public PushResult sendPush(String deviceToken, PushMessage message) {
        sent.add(new Sent(deviceToken, message));
        return resultOf(deviceToken);
    }

    @Override
    public Map<String, PushResult> sendMulticast(List<String> deviceTokens, PushMessage message) {
        Map<String, PushResult> multicastResults = new LinkedHashMap<>();
        deviceTokens.forEach(token -> {
            sent.add(new Sent(token, message));
            multicastResults.put(token, resultOf(token));
        });
        return multicastResults;
    }

    /** 이 토큰으로 보내면 이 결과를 돌려주게 한다. 지정하지 않으면 SUCCESS. */
    public void willReturn(String deviceToken, PushResult result) {
        results.put(deviceToken, result);
    }

    public List<Sent> sent() {
        return sent;
    }

    /** 컨텍스트는 테스트 클래스 단위로 재사용되므로 매 테스트 전에 비운다. */
    public void reset() {
        sent.clear();
        results.clear();
    }

    private PushResult resultOf(String deviceToken) {
        return results.getOrDefault(deviceToken, PushResult.SUCCESS);
    }
}