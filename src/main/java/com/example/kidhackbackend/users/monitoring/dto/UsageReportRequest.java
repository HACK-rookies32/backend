package com.example.kidhackbackend.users.monitoring.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 앱 사용량 배치 전송 (자녀 앱→서버, No.13).
 * 위치 동선처럼 여러 건을 한 번에 전송한다.
 */
public record UsageReportRequest(
        @NotNull Long childId,
        @NotEmpty @Valid List<UsageItem> usages
) {
    public record UsageItem(
            @NotBlank String appName,
            // [VULN-MON-2] durationSec 검증 누락(음수·과대값 허용) → 사용량 통계 조작 가능
            long durationSec,
            @NotNull LocalDateTime recordedAt
    ) {
    }
}
