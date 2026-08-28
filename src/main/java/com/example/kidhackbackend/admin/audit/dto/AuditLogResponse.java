package com.example.kidhackbackend.admin.audit.dto;

import com.example.kidhackbackend.admin.audit.entity.AdminAuditLog;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 활동 이력 1건 응답.
 *
 * <p>{@code activityType} 은 배지 스타일 분기용 코드, {@code activityLabel} 은 그대로 화면에 찍을 문구.
 */
@Schema(description = "관리자 활동 이력 1건")
public record AuditLogResponse(
        @Schema(description = "로그 ID", example = "1") Long id,
        @Schema(description = "관리자 회원 ID", example = "2") Long adminId,
        @Schema(description = "관리자 로그인 아이디", example = "admin01") String adminLoginId,
        @Schema(description = "활동 유형 코드", example = "NOTICE_CREATE") String activityType,
        @Schema(description = "활동 유형 표시 문구", example = "공지 생성") String activityLabel,
        @Schema(description = "대상 리소스 유형", example = "NOTICE") String targetType,
        @Schema(description = "대상 리소스 ID(전체 대상이면 null)", example = "10") Long targetId,
        @Schema(description = "발생 시각") LocalDateTime occurredAt
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
