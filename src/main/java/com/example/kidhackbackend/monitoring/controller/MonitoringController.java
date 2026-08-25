package com.example.kidhackbackend.monitoring.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.monitoring.dto.GroupBy;
import com.example.kidhackbackend.monitoring.dto.UsageAggregateResponse;
import com.example.kidhackbackend.monitoring.dto.UsageReportRequest;
import com.example.kidhackbackend.monitoring.service.AppUsageService;
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
@Tag(name = "Monitoring", description = "앱 사용량 모니터링 API — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final AppUsageService appUsageService;

    /** 자녀 앱이 사용량을 배치 전송. */
    // [VULN-MON-1] 본문 childId 소유권 미검증 → 남의 자녀 사용량 위조 주입 가능.
    @PostMapping("/usage")
    public ResponseEntity<ApiResponse<Integer>> report(@Valid @RequestBody UsageReportRequest request) {
        int saved = appUsageService.report(request);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    /** 부모가 사용량 집계 조회. groupBy=HOUR/APP/DAY, from/to 없으면 최근 1주일. */
    // [VULN-MON-1] 경로 childId 소유권 미검증 → 남의 자녀 사용량 열람 가능(IDOR).
    @GetMapping("/usage/{childId}")
    public ResponseEntity<ApiResponse<UsageAggregateResponse>> aggregate(
            @PathVariable Long childId,
            @RequestParam(defaultValue = "DAY") GroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        UsageAggregateResponse data = appUsageService.aggregate(childId, groupBy, from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
