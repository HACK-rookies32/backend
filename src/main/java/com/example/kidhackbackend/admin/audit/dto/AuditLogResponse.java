package com.example.kidhackbackend.admin.audit.dto;

import com.example.kidhackbackend.admin.audit.entity.AdminAuditLog;
import java.time.LocalDateTime;

/**
 * 활동 이력 1건 응답.
 *
 * <p>{@code activityType} 은 프론트가 배지 스타일을 분기할 코드, {@code activityLabel} 은 그대로
 * 화면에 찍을 문구다. 표시 문구를 서버가 확정해 내려 주므로 프론트에 코드→한글 매핑표가 없어도 된다.
 */
public record AuditLogResponse(
        Long id,
        Long adminId,
        String adminLoginId,
        String activityType,
        String activityLabel,
        String targetType,
        Long targetId,
        LocalDateTime occurredAt
) {
    public static AuditLogResponse from(AdminAuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAdminId(),
                log.getAdminLoginId(),
                log.getActivityType().name(),
                log.getActivityType().getLabel(),
                log.getTargetType().name(),
                log.getTargetId(),
                log.getOccurredAt());
    }
}
