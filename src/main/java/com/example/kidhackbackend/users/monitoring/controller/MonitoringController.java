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
import org.springframework.http.HttpStatus;
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
            description = """
                    자녀 앱이 앱별 사용시간을 모아 전송한다(한 요청 1~500건). 저장된 건수를 반환한다.

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `childId` | ✅ | 자녀 회원 ID |
                    | `usages` | ✅ | 사용 기록 목록(1~500건) |
                    | `usages[].appName` | ✅ | 앱 이름 |
                    | `usages[].durationSec` | ✅ | 사용시간(초) — ⚠️ 음수·과대값 검증 없음 |
                    | `usages[].recordedAt` | ✅ | 기록 시각(ISO-8601) |

                    ⚠️ [VULN-MON-1] 본문 childId 소유권 미검증(IDOR). [VULN-MON-2] durationSec 미검증. [VULN-MON-3] 호출 rate limit 없음.""")
    @PostMapping("/usage")
    public ResponseEntity<ApiResponse<Integer>> report(@Valid @RequestBody UsageReportRequest request) {
        int saved = appUsageService.report(request);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    @Operation(summary = "사용량 집계 조회 (부모)",
            description = """
                    groupBy 로 사용시간(초)을 합산한다. from/to 없으면 최근 1주일. 결과는 key 오름차순.

                    - `DAY`  : 일자별 (yyyy-MM-dd)
                    - `APP`  : 앱 이름별
                    - `HOUR` : 시간대별(00~23). 여러 날을 조회하면 **요일 무관하게 같은 시간대끼리 합산**된다.

                    ⚠️ [VULN-MON-1] 경로 childId 소유권 미검증(IDOR).
                    """)
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

    @Operation(summary = "사용량 즉시 보고 요청 (부모)",
            description = """
                    자녀 앱에 「사용량을 지금 올려라」 푸시를 보내고 202. 실제 값은 자녀 앱이
                    곧이어 `POST /api/monitoring/usage` 로 올려 완성된다.

                    자녀 앱은 평소 1시간 주기로만 올리므로, 부모가 사용 리포트를 새로고침할 때
                    이 요청을 함께 보내야 방금까지의 사용시간이 반영된다. 자녀 기기가 꺼져 있거나
                    네트워크가 없으면 요청은 닿지 않고 마지막 값이 그대로 남는다.

                    ⚠️ [VULN-MON-1] 경로 childId 소유권 미검증 → 남의 자녀 기기에 보고 트리거 가능(IDOR), rate limit 없음.""")
    @PostMapping("/usage/{childId}/request")
    public ResponseEntity<ApiResponse<Void>> requestReport(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        appUsageService.requestReport(childId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(null));
    }
}
