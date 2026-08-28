package com.example.kidhackbackend.users.location.dto;

import com.example.kidhackbackend.users.location.entity.TrackingSetting;
import io.swagger.v3.oas.annotations.media.Schema;

/** 실시간 추적 설정 응답 DTO. */
@Schema(description = "실시간 추적 설정")
public record TrackingSettingResponse(
        @Schema(description = "자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "실시간 추적 on/off", example = "true") boolean realtimeEnabled
) {
    public static TrackingSettingResponse from(TrackingSetting s) {
        return new TrackingSettingResponse(s.getChildId(), s.isRealtimeEnabled());
    }
}
