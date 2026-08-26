package com.example.kidhackbackend.global.notification.service;

import com.example.kidhackbackend.global.notification.entity.Notification;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.push.PushPort;
import com.example.kidhackbackend.global.notification.repository.DeviceTokenRepository;
import com.example.kidhackbackend.global.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationPort {

    private final NotificationRepository repository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationSettingService notificationSettingService;
    private final PushPort pushPort;

    @Override
    @Transactional
    public void publish(Long recipientId, NotificationType type, String message) {
        if (!notificationSettingService.isEnabled(recipientId, type)) {
            log.info("알림 설정이 꺼져있어 발송 생략: recipientId={}, type={}", recipientId, type);
            return;
        }

        repository.save(new Notification(recipientId, type, message));
        sendPush(recipientId, type, message);
    }

    private void sendPush(Long recipientId, NotificationType type, String message) {
        deviceTokenRepository.findByMemberId(recipientId)
                .ifPresent(deviceToken -> pushPort.sendPush(deviceToken.getToken(), type.name(), message));
    }

    @Transactional(readOnly = true)
    public List<Notification> findByRecipient(Long recipientId) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다: " + notificationId));
        notification.markAsRead();
    }

    @Transactional(readOnly = true)
    public long countUnread(Long recipientId) {
        return repository.countByRecipientIdAndReadFalse(recipientId);
    }
}