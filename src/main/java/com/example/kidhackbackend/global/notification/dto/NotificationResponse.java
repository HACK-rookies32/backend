package com.example.kidhackbackend.global.notification.dto;

import com.example.kidhackbackend.global.notification.entity.Notification;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 알림 응답 DTO. */
@Schema(description = "알림")
public record NotificationResponse(
        @Schema(description = "알림 ID", example = "1") Long id,
        @Schema(description = "수신자 회원 ID", example = "7") Long recipientId,
        @Schema(description = "알림 유형", example = "LOCATION_ENTER") NotificationType type,
        @Schema(description = "알림 메시지", example = "'학원'에 진입했습니다.") String message,
        @Schema(description = "읽음 여부", example = "false") boolean read,
        @Schema(description = "생성 시각") LocalDateTime createdAt
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
