package com.example.kidhackbackend.global.notification.dto;

import com.example.kidhackbackend.global.notification.entity.DeviceToken;

import java.time.LocalDateTime;

public record DeviceTokenResponse(
        Long memberId,
        String token,
        LocalDateTime updatedAt
) {
    public static DeviceTokenResponse from(DeviceToken deviceToken) {
        return new DeviceTokenResponse(
                deviceToken.getMemberId(),
                deviceToken.getToken(),
                deviceToken.getUpdatedAt()
        );
    }
}