package com.example.kidhackbackend.users.device.dto;

import com.example.kidhackbackend.users.device.entity.DeviceEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 기기 전원/GPS 이벤트 보고 (No.4). */
@Schema(description = "기기 전원/GPS 이벤트 보고 요청")
public record DeviceEventRequest(
        @Schema(description = "자녀 회원 ID", example = "7") @NotNull Long childId,
        @Schema(description = "기기 식별자", example = "device-abc-123") @NotBlank String deviceId,
        @Schema(description = "이벤트 유형", example = "POWER_OFF") @NotNull DeviceEventType type
) {
}
