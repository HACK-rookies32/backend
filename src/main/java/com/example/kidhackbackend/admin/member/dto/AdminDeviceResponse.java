package com.example.kidhackbackend.admin.member.dto;

import com.example.kidhackbackend.users.device.entity.DeviceEvent;
import com.example.kidhackbackend.users.device.entity.DeviceEventType;

import java.time.LocalDateTime;

public record AdminDeviceResponse(
        String deviceId, DeviceEventType lastEventType, LocalDateTime lastEventAt
) {
    public static AdminDeviceResponse from(DeviceEvent event) {
        return new AdminDeviceResponse(event.getDeviceId(), event.getType(), event.getCreatedAt());
    }
}