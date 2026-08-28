package com.example.kidhackbackend.admin.member.dto;

import com.example.kidhackbackend.users.device.entity.DeviceEvent;
import com.example.kidhackbackend.users.device.entity.DeviceEventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 - 고객 기기(최근 이벤트 기준)")
public record AdminDeviceResponse(
        @Schema(description = "기기 식별자", example = "device-abc-123") String deviceId,
        @Schema(description = "마지막 이벤트 유형", example = "POWER_OFF") DeviceEventType lastEventType,
        @Schema(description = "마지막 이벤트 시각") LocalDateTime lastEventAt
) {
    public static AdminDeviceResponse from(DeviceEvent event) {
        return new AdminDeviceResponse(event.getDeviceId(), event.getType(), event.getCreatedAt());
    }
}
