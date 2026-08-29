package com.example.kidhackbackend.global.notification.controller;

import com.example.kidhackbackend.global.notification.dto.DeviceTokenRequest;
import com.example.kidhackbackend.global.notification.dto.DeviceTokenResponse;
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

import java.util.List;

/**
 * FCM 토큰 등록 API (공통 도메인).
 *
 * <p>기기(토큰)당 한 행이며 한 계정이 여러 기기를 등록할 수 있다. 앱은 로그인 직후와
 * FCM {@code onTokenRefresh} 때 등록, 로그아웃 때 해제를 호출한다.
 */
@Tag(name = "25. FCM 토큰", description = "FCM 푸시 토큰 등록/해제 · 내 토큰 조회 — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @Operation(summary = "FCM 토큰 등록/갱신",
            description = "로그인한 회원의 FCM 디바이스 토큰을 등록하거나 갱신한다. 같은 토큰이 다른 계정에 물려 있으면 이 회원으로 주인을 옮긴다.")
    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> register(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.register(member.getId(), request.fcmToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "FCM 토큰 해제 (로그아웃)",
            description = "본문에 담은 토큰이 내 것이면 삭제한다. 없어도 200(멱등).")
    @DeleteMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> unregister(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.unregister(member.getId(), request.fcmToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "내 FCM 토큰 목록 조회",
            description = "등록된 토큰이 없으면 빈 배열.")
    @GetMapping("/fcm-token")
    public ResponseEntity<ApiResponse<List<DeviceTokenResponse>>> getMyTokens(
            @AuthenticationPrincipal Member member) {
        List<DeviceTokenResponse> tokens = deviceTokenService.getMyTokens(member.getId()).stream()
                .map(DeviceTokenResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }
}
