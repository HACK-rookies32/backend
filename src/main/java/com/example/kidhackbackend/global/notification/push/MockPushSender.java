package com.example.kidhackbackend.global.notification.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.push.provider", havingValue = "mock", matchIfMissing = true)
public class MockPushSender implements PushPort {
    @Override
    public void sendPush(String deviceToken, String title, String body) {
        log.info("[MOCK PUSH] token={}, title={}, body={}", deviceToken, title, body);
    }
}