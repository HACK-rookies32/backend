package com.example.kidhackbackend.monitoring.dto;

import java.util.List;

/** 사용량 집계 응답 (No.13). */
public record UsageAggregateResponse(
        Long childId,
        GroupBy groupBy,
        List<Bucket> items
) {
    /** 집계 단위(키: 시간대/앱/일자, 값: 사용시간 합계 초). */
    public record Bucket(String key, long durationSec) {
    }
}
