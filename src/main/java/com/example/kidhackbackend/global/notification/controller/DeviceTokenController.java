package com.example.kidhackbackend.global.notification.controller;

import com.example.kidhackbackend.global.notification.dto.DeviceTokenRequest;
import com.example.kidhackbackend.global.notification.dto.DeviceTokenResponse;
import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import com.example.kidhackbackend.global.notification.service.DeviceTokenService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "25. FCM 토큰", description = "FCM 푸시 토큰 등록 · 내 토큰 조회 — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @Operation(summary = "FCM 토큰 등록/갱신",
            description = "로그인한 회원의 FCM 디바이스 토큰을 등록하거나 갱신한다.")
    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> register(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.register(member.getId(), request.fcmToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "내 FCM 토큰 조회",
            description = "등록된 토큰이 없으면 `data` 는 null.")
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
