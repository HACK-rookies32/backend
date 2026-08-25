package com.example.kidhackbackend.global.notification.controller;

import com.example.kidhackbackend.global.notification.dto.DeviceTokenRequest;
import com.example.kidhackbackend.global.notification.dto.DeviceTokenResponse;
import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import com.example.kidhackbackend.global.notification.service.DeviceTokenService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * FCM 토큰 등록 API (공통 도메인).
 */
@Tag(name = "DeviceToken", description = "FCM 푸시 토큰 등록 API — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> register(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.register(member.getId(), request.fcmToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    @GetMapping("/fcm-token")
    public ResponseEntity<ApiResponse<DeviceTokenResponse>> getMyToken(
            @AuthenticationPrincipal Member member
    ) {
        DeviceToken token = deviceTokenService.getMyToken(member.getId());
        if (token == null) {
            return ResponseEntity.ok(ApiResponse.success(null)); // 아직 등록 안 된 상태
        }
        return ResponseEntity.ok(ApiResponse.success(DeviceTokenResponse.from(token)));
    }
}