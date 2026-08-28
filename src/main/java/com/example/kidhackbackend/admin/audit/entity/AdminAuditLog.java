package com.example.kidhackbackend.admin.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 활동 이력 1건. 추가만 되고 수정·삭제되지 않는다(append-only).
 *
 * <p>수정이 없으므로 {@code BaseEntity}(createdAt/updatedAt) 를 상속하지 않고 발생 시각만
 * {@code occurredAt} 으로 직접 들고 있다. 같은 이유로 setter 도 두지 않는다.
 *
 * <p>{@code adminLoginId} 는 활동 시점의 로그인 ID 를 <b>복제</b>해 둔 값이다. 관리자 계정이 나중에
 * 삭제되거나 ID 가 바뀌어도 "누가 했는지" 가 남아야 부인방지가 성립한다. 그래서 member 로 조인하지
 * 않고 값으로 박아 넣는다(조회 시 조인이 사라져 페이징도 가벼워진다).
 *
 * <p>테이블명 주의: Oracle 예약어 {@code AUDIT} 를 피해 {@code admin_audit_log} 로 둔다.
 */
@Entity
@Table(name = "admin_audit_log",
        indexes = {
                @Index(name = "idx_admin_audit_log_occurred", columnList = "occurred_at"),
                @Index(name = "idx_admin_audit_log_admin", columnList = "admin_login_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 활동을 수행한 관리자 회원 식별 ID. */
    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    /** 활동 시점의 관리자 로그인 ID (계정이 사라져도 남는 스냅샷). */
    @Column(name = "admin_login_id", nullable = false)
    private String adminLoginId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private AdminActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private AuditTargetType targetType;

    /** 대상 리소스 식별 ID. 전체 알림 발송처럼 특정 대상이 없는 활동은 null. */
    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /**
     * 활동 사유·설명. 관리자가 입력한 문구를 그대로 남긴다(예: 충전 취소 사유).
     *
     * <p>사유를 받지 않는 활동이 대부분이라 nullable 이다 — 기존에 쌓인 이력도 전부 null 로 남는다.
     * 사유가 <b>필수</b>인 활동(충전 취소)은 컨트롤러에서 {@code @NotBlank} 로 걸러 여기까지 빈 값이
     * 내려오지 않게 한다.
     */
    @Column(name = "description", length = 500)
    private String description;

    private AdminAuditLog(Long adminId, String adminLoginId, AdminActivityType activityType, Long targetId,
                          String description) {
        this.adminId = adminId;
        this.adminLoginId = adminLoginId;
        this.activityType = activityType;
        this.targetType = activityType.getTargetType();
        this.targetId = targetId;
        this.description = description;
        this.occurredAt = LocalDateTime.now();
    }

    /** 사유를 받지 않는 활동용(기존 호출부 전부 이쪽). {@code description} 은 null 로 남는다. */
    public static AdminAuditLog of(Long adminId, String adminLoginId, AdminActivityType activityType, Long targetId) {
        return new AdminAuditLog(adminId, adminLoginId, activityType, targetId, null);
    }

    /** 사유를 함께 남기는 활동용(충전 취소 등). */
    public static AdminAuditLog of(Long adminId, String adminLoginId, AdminActivityType activityType, Long targetId,
                                   String description) {
        return new AdminAuditLog(adminId, adminLoginId, activityType, targetId, description);
    }
}
