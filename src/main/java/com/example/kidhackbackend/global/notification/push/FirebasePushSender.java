package com.example.kidhackbackend.global.notification.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.push.provider", havingValue = "firebase")
public class FirebasePushSender implements PushPort {

    @Override
    public void sendPush(String deviceToken, String title, String body) {
        try {
            Message fcmMessage = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                    .build();
            FirebaseMessaging.getInstance().send(fcmMessage);
        } catch (FirebaseMessagingException e) {
            // 토큰 만료/무효 등 — 발송 실패해도 알림 저장 자체는 이미 끝났으므로 로그만 남기고 넘어감
            log.warn("FCM 발송 실패: deviceToken={}, error={}", deviceToken, e.getMessage());
        }
    }
}