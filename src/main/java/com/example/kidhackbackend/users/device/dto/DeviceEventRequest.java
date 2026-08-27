package com.example.kidhackbackend.users.device.dto;

import com.example.kidhackbackend.users.device.entity.DeviceEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 기기 전원/GPS 이벤트 보고 (No.4). */
public record DeviceEventRequest(
        @NotNull Long childId,
        @NotBlank String deviceId,
        @NotNull DeviceEventType type
) {
}