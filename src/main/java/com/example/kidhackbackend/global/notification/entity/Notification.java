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
        indexes = {
                @Index(name = "idx_notification_recipient", columnList = "recipientId,createdAt"),
                @Index(name = "idx_notification_child", columnList = "recipientId,childId,createdAt")
        }
)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림을 받는 사용자(예: 부모). */
    @Column(nullable = false)
    private Long recipientId;

    /**
     * 이 알림이 <b>어느 자녀에 대한 것인지</b>. 수신자({@link #recipientId})와는 다르다.
     *
     * <p>자녀를 여럿 둔 보호자가 목록에서 자녀별로 걸러 볼 수 있게 하려고 둔다. 예전에는
     * 이 값이 없어 본문으로도 구분할 수 없었다("'학원'에 진입했습니다." 에는 자녀 이름이 없다).
     *
     * <p>nullable 인 이유: 자녀 맥락이 없는 알림이 있다(1:1 문의 답변, 관리자 전체 발송).
     * 이 컬럼이 생기기 전에 쌓인 행도 null 이다.
     */
    private Long childId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /**
     * 알림 제목. 단건 알림은 {@link NotificationType#getTitle()} 을 그대로 쓰고,
     * 관리자 전체 알림은 관리자가 입력한 제목이 들어온다.
     *
     * <p>nullable 인 이유: 이 컬럼이 생기기 전에 쌓인 행이 있다(V4 이전). 새로 만드는
     * 행은 생성자가 항상 채운다.
     */
    private String title;

    @Column(nullable = false)
    private String message;

    /** 읽음 여부. ("read"는 일부 DB 예약어라 컬럼명을 is_read로 둔다) */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    /** 자녀 맥락이 없는 단건 알림. 제목은 유형이 정한 것을 쓴다. */
    public Notification(Long recipientId, NotificationType type, String message) {
        this(recipientId, null, type, type.getTitle(), message);
    }

    /** 자녀에 대한 단건 알림. 제목은 유형이 정한 것을 쓴다. */
    public Notification(Long recipientId, Long childId, NotificationType type, String message) {
        this(recipientId, childId, type, type.getTitle(), message);
    }

    /** 제목을 직접 정하는 알림(관리자 전체 알림). 자녀 맥락 없음. */
    public Notification(Long recipientId, NotificationType type, String title, String message) {
        this(recipientId, null, type, title, message);
    }

    public Notification(Long recipientId, Long childId, NotificationType type,
                        String title, String message) {
        this.recipientId = recipientId;
        this.childId = childId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = false;
    }

    /** 읽음 처리. */
    public void markAsRead() {
        this.read = true;
    }
}
