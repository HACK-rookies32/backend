package com.example.kidhackbackend.global.notification.service;

import com.example.kidhackbackend.global.notification.entity.Notification;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.repository.DeviceTokenRepository;
import com.example.kidhackbackend.global.notification.repository.NotificationRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 저장/조회 서비스. {@link NotificationPort}의 구현체로, 다른 도메인의 알림 생성을 처리한다.
 * DB 저장 후 등록된 FCM 토큰이 있으면 실제 푸시도 함께 발송한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationPort {

    private final NotificationRepository repository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    @Transactional
    public void publish(Long recipientId, NotificationType type, String message) {
        repository.save(new Notification(recipientId, type, message));
        sendPush(recipientId, type, message);
    }

    private void sendPush(Long recipientId, NotificationType type, String message) {
        deviceTokenRepository.findByMemberId(recipientId).ifPresent(deviceToken -> {
            try {
                Message fcmMessage = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(
                                com.google.firebase.messaging.Notification.builder()
                                        .setTitle(type.name())
                                        .setBody(message)
                                        .build())
                        .build();
                FirebaseMessaging.getInstance().send(fcmMessage);
            } catch (FirebaseMessagingException e) {
                // 토큰 만료/무효 등 — 발송 실패해도 알림 저장 자체는 이미 끝났으므로 로그만 남기고 넘어감
                log.warn("FCM 발송 실패: recipientId={}, error={}", recipientId, e.getMessage());
            }
        });
    }

    /** 수신자별 알림 목록(최신순). */
    @Transactional(readOnly = true)
    public List<Notification> findByRecipient(Long recipientId) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    /** 알림 읽음 처리. */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다: " + notificationId));
        notification.markAsRead();
    }
}