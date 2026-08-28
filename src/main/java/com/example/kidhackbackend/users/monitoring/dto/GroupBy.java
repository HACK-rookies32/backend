package com.example.kidhackbackend.users.monitoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 사용량 집계 기준 (No.13). */
@Schema(description = "사용량 집계 기준")
public enum GroupBy {
    @Schema(description = "시간대별(0~23시)") HOUR,
    @Schema(description = "앱별") APP,
    @Schema(description = "일별(yyyy-MM-dd)") DAY
}
