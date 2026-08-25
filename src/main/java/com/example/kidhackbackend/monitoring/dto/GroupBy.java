package com.example.kidhackbackend.monitoring.dto;

/** 사용량 집계 기준 (No.13). */
public enum GroupBy {
    HOUR,   // 시간대별 (0~23시)
    APP,    // 앱별
    DAY     // 일별 (yyyy-MM-dd)
}
