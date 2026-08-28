package com.example.kidhackbackend.global.notification.dto;

import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "등록된 FCM 토큰")
public record DeviceTokenResponse(
        @Schema(description = "회원 ID", example = "1") Long memberId,
        @Schema(description = "FCM 토큰") String token,
        @Schema(description = "갱신 시각") LocalDateTime updatedAt
) {
    public static DeviceTokenResponse from(DeviceToken deviceToken) {
        return new DeviceTokenResponse(
                deviceToken.getMemberId(),
                deviceToken.getToken(),
                deviceToken.getUpdatedAt()
        );
    }
}
