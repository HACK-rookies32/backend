package com.example.kidhackbackend.location.dto;

import com.example.kidhackbackend.location.entity.TrackingSetting;

/** 실시간 추적 설정 응답 DTO. */
public record TrackingSettingResponse(
        Long childId,
        boolean realtimeEnabled
) {
    public static TrackingSettingResponse from(TrackingSetting s) {
        return new TrackingSettingResponse(s.getChildId(), s.isRealtimeEnabled());
    }
}
