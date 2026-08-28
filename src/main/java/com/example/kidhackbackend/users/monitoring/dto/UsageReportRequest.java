package com.example.kidhackbackend.users.monitoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 앱 사용량 배치 전송 (자녀 앱→서버, No.13).
 * 위치 동선처럼 여러 건을 한 번에 전송한다.
 */
@Schema(description = "앱 사용량 배치 전송 요청")
public record UsageReportRequest(
        @Schema(description = "자녀 회원 ID", example = "7") @NotNull Long childId,
        @Schema(description = "사용 기록 목록(1~500건)")
        @NotEmpty @Size(max = 500, message = "한 번에 500건까지 전송할 수 있습니다.") @Valid List<UsageItem> usages
) {
    @Schema(description = "앱별 사용 기록 1건")
    public record UsageItem(
            @Schema(description = "앱 이름", example = "YouTube") @NotBlank String appName,
            // [VULN-MON-2] durationSec 검증 누락(음수·과대값 허용) → 사용량 통계 조작 가능
            @Schema(description = "사용시간(초). ⚠️ 음수·과대값 검증 없음(의도된 취약점)", example = "1800") long durationSec,
            @Schema(description = "기록 시각") @NotNull LocalDateTime recordedAt
    ) {
    }
}
