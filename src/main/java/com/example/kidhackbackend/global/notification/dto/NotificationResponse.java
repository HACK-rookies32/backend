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
        @Schema(description = "알림 대상 자녀 회원 ID. 자녀 맥락이 없는 알림(문의 답변·전체 발송)은 null",
                example = "12", nullable = true) Long childId,
        @Schema(description = "알림 유형", example = "LOCATION_ENTER") NotificationType type,
        @Schema(description = "알림 제목(푸시 알림창에 떴던 것과 동일)", example = "장소 진입") String title,
        @Schema(description = "알림 메시지", example = "'학원'에 진입했습니다.") String message,
        @Schema(description = "읽음 여부", example = "false") boolean read,
        @Schema(description = "생성 시각") LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientId(),
                n.getChildId(),
                n.getType(),
                // V4 이전에 쌓인 행은 title 이 null 이라 유형 기본 제목으로 채운다.
                n.getTitle() != null ? n.getTitle() : n.getType().getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
