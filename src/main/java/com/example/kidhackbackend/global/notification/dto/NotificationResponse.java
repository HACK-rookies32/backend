package com.example.kidhackbackend.global.notification.dto;

import com.example.kidhackbackend.global.notification.entity.Notification;
import com.example.kidhackbackend.global.notification.entity.NotificationType;

import java.time.LocalDateTime;

/** 알림 응답 DTO. */
public record NotificationResponse(
        Long id,
        Long recipientId,
        NotificationType type,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientId(),
                n.getType(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
