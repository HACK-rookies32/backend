package com.example.kidhackbackend.users.device.controller;

import com.example.kidhackbackend.users.device.dto.DeviceEventRequest;
import com.example.kidhackbackend.users.device.service.DeviceEventService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기기 전원/GPS 감지 API — 역할표 No.4.
 */
@Tag(name = "Device", description = "기기 전원/GPS 감지 API — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceEventService deviceEventService;

    /** 자녀 앱이 전원/GPS 꺼짐을 보고 → '트래킹 불가' 알림 발행. */
    @PostMapping("/event")
    public ResponseEntity<ApiResponse<Void>> report(@Valid @RequestBody DeviceEventRequest request) {
        deviceEventService.report(request.childId(), request.type());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
