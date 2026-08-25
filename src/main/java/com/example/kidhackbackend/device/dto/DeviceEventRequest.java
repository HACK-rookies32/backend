package com.example.kidhackbackend.device.dto;

import com.example.kidhackbackend.device.entity.DeviceEventType;
import jakarta.validation.constraints.NotNull;

/** 기기 전원/GPS 이벤트 보고 (No.4). */
public record DeviceEventRequest(
        @NotNull Long childId,
        @NotNull DeviceEventType type
) {
}
