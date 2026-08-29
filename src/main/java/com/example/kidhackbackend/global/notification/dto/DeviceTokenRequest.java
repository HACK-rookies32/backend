package com.example.kidhackbackend.global.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "FCM 토큰 등록/해제 요청")
public record DeviceTokenRequest(
        @Schema(description = "FCM 디바이스 토큰")
        @NotBlank
        @Size(max = 500, message = "FCM 토큰이 너무 깁니다(최대 500자).")
        String fcmToken
) {
}
