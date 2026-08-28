package com.example.kidhackbackend.users.monitoring.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.monitoring.dto.GroupBy;
import com.example.kidhackbackend.users.monitoring.dto.UsageAggregateResponse;
import com.example.kidhackbackend.users.monitoring.dto.UsageReportRequest;
import com.example.kidhackbackend.users.monitoring.service.AppUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 사용 모니터링 API — 역할표 No.13.
 */
@Tag(name = "08. 사용 모니터링", description = "자녀 앱이 사용량 배치 전송 · 부모가 사용량 집계 조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final AppUsageService appUsageService;

    @Operation(summary = "사용량 배치 전송 (자녀 앱)",
            description = "자녀 앱이 앱별 사용시간을 모아 전송한다. 저장된 건수를 반환한다. ⚠️ 본문 childId 소유권 미검증(IDOR).")
    @PostMapping("/usage")
    public ResponseEntity<ApiResponse<Integer>> report(@Valid @RequestBody UsageReportRequest request) {
        int saved = appUsageService.report(request);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @Operation(summary = "사용량 집계 조회 (부모)",
            description = "groupBy=HOUR/APP/DAY 로 집계한다. from/to 없으면 최근 1주일. ⚠️ 경로 childId 소유권 미검증(IDOR).")
    @GetMapping("/usage/{childId}")
    public ResponseEntity<ApiResponse<UsageAggregateResponse>> aggregate(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId,
            @Parameter(description = "집계 기준: HOUR / APP / DAY") @RequestParam(defaultValue = "DAY") GroupBy groupBy,
            @Parameter(description = "조회 시작(ISO-8601). 생략 시 최근 1주일")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "조회 종료(ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        UsageAggregateResponse data = appUsageService.aggregate(childId, groupBy, from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
