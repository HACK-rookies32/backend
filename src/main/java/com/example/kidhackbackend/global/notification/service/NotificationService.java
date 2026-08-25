package com.example.kidhackbackend.global.notification.service;

import com.example.kidhackbackend.global.notification.entity.Notification;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 저장/조회 서비스. {@link NotificationPort}의 구현체로, 다른 도메인의 알림 생성을 처리한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationPort {

    private final NotificationRepository repository;

    @Override
    @Transactional
    public void publish(Long recipientId, NotificationType type, String message) {
        repository.save(new Notification(recipientId, type, message));
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
