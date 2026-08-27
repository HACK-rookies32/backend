package com.example.kidhackbackend.admin.audit.controller;

import com.example.kidhackbackend.admin.audit.dto.AuditLogResponse;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 활동 이력 조회 API (SUPER_ADMIN 전용).
 *
 * <p>시큐리티는 {@code AdminAuditLogSecurityConfig}(/api/admin/audit-logs/**) 가 담당한다.
 */
@Tag(name = "Admin Audit Log", description = "관리자 활동 이력 조회 API (SUPER_ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    /**
     * 조건별 활동 이력 조회. 페이지당 20건 고정, 발생일시 내림차순.
     *
     * <p>{@code activityType} 을 열거형이 아니라 문자열로 받는 이유: 정의되지 않은 코드를
     * {@code INVALID_PARAMETER} 가 아니라 {@code INVALID_ACTIVITY_TYPE} 으로 돌려줘야 해서
     * 스프링의 파라미터 바인딩이 아니라 서비스에서 직접 변환한다.
     *
     * <p>반대로 날짜는 {@code LocalDate} 로 받는다. 형식이 어긋나면 바인딩 단계에서 걸려
     * {@code INVALID_PARAMETER} 로 나가는데, 그게 명세서가 요구하는 코드다.
     */
    @Operation(summary = "관리자 활동 이력 조회",
            description = "관리자 로그인 ID(부분 일치)·활동 유형·기간으로 걸러 페이징 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String adminLoginId,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page) {

        return ResponseEntity.ok(ApiResponse.success(
                adminAuditLogService.search(adminLoginId, activityType, from, to, page)));
    }
}
