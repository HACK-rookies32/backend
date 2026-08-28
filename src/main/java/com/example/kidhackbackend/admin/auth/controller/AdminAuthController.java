package com.example.kidhackbackend.admin.auth.controller;

import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.dto.LoginRequest;
import com.example.kidhackbackend.users.member.dto.LoginResponse;
import com.example.kidhackbackend.users.member.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "30. 관리자 - 로그인", description = "관리자 로그인 (SUPER/GENERAL/COMMUNITY/INQUIRY_ADMIN)")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final AdminAuditLogService adminAuditLogService;

    @Operation(summary = "관리자 로그인",
            description = """
                    `loginId` · `password` 로 로그인한다. 관리자 역할(SUPER/GENERAL/COMMUNITY/INQUIRY_ADMIN)이 아니면 `403`.
                    성공 시 감사 로그를 남긴다. 응답 형태는 일반 로그인과 동일(`token`, `refreshToken`, `loginId`, `username`, `role`).
                    """)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.adminLogin(request);
        // 로그인이 성공한 뒤에만 남긴다 — 실패 시도는 adminLogin 이 예외로 끊는다.
        adminAuditLogService.recordAdminLogin(response.getLoginId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
