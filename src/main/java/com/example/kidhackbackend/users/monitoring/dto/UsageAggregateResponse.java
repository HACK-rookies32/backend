package com.example.kidhackbackend.users.monitoring.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 사용량 집계 응답 (No.13). */
@Schema(description = "앱 사용량 집계 결과")
public record UsageAggregateResponse(
        @Schema(description = "자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "집계 기준") GroupBy groupBy,
        @Schema(description = "집계 버킷 목록") List<Bucket> items
) {
    /** 집계 단위(키: 시간대/앱/일자, 값: 사용시간 합계 초). */
    @Schema(description = "집계 버킷")
    public record Bucket(
            @Schema(description = "버킷 키(시간대/앱명/일자)", example = "2026-08-27") String key,
            @Schema(description = "사용시간 합계(초)", example = "3600") long durationSec
    ) {
    }
}
