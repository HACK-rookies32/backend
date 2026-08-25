package com.example.kidhackbackend.global.notification.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림(공통 도메인).
 *
 * <p>여러 도메인(위치·전원감지·유해사이트 등)에서 발생한 알림을 한 곳에 저장한다.
 * 생성/수정 시각은 {@link BaseEntity}(JPA Auditing)로 자동 기록된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notification",
        indexes = @Index(name = "idx_notification_recipient", columnList = "recipientId,createdAt")
)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림을 받는 사용자(예: 부모). */
    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String message;

    /** 읽음 여부. ("read"는 일부 DB 예약어라 컬럼명을 is_read로 둔다) */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    public Notification(Long recipientId, NotificationType type, String message) {
        this.recipientId = recipientId;
        this.type = type;
        this.message = message;
        this.read = false;
    }

    /** 읽음 처리. */
    public void markAsRead() {
        this.read = true;
    }
}
