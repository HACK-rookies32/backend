package com.example.kidhackbackend.users.device.dto;

import com.example.kidhackbackend.users.device.entity.DeviceEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 기기 전원/GPS 이벤트 보고 (No.4). 모든 필드 필수. */
@Schema(description = "기기 전원/GPS 이벤트 보고 요청")
public record DeviceEventRequest(
        @Schema(description = "(필수) 자녀 회원 ID", example = "7", requiredMode = RequiredMode.REQUIRED)
        @NotNull Long childId,
        @Schema(description = "(필수) 기기 식별자", example = "device-abc-123", requiredMode = RequiredMode.REQUIRED)
        @NotBlank String deviceId,
        @Schema(description = "(필수) 이벤트 유형: POWER_OFF / GPS_OFF", example = "POWER_OFF",
                requiredMode = RequiredMode.REQUIRED)
        @NotNull DeviceEventType type
) {
}
