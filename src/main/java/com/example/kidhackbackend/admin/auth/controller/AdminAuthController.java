package com.example.kidhackbackend.admin.auth.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.dto.LoginRequest;
import com.example.kidhackbackend.users.member.dto.LoginResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "30. 관리자 - 로그인", description = "관리자 로그인/로그아웃 (SUPER/GENERAL/COMMUNITY/INQUIRY_ADMIN)")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final AdminAuditLogService adminAuditLogService;

    @Operation(summary = "관리자 로그인",
            description = """
                    `loginId` · `password` 로 로그인한다. 성공 시 감사 로그(`ADMIN_LOGIN`)를 남긴다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    | --- | --- | --- |
                    | `loginId` | ✅ | 관리자 로그인 아이디 |
                    | `password` | ✅ | 비밀번호 |

                    **응답 필드** — `token` · `refreshToken` · `loginId` · `username` · `role` (일반 로그인과 동일)

                    - 아이디 없음·비밀번호 불일치: `401 UNAUTHORIZED`.
                    - 관리자 역할(SUPER/GENERAL/COMMUNITY/INQUIRY_ADMIN)이 아니면 `403 FORBIDDEN`.
                    """)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.adminLogin(request);
        // 로그인이 성공한 뒤에만 남긴다 — 실패 시도는 adminLogin 이 예외로 끊는다.
        adminAuditLogService.recordAdminLogin(response.getLoginId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "관리자 로그아웃",
            description = """
                    로그인한 관리자 본인의 refresh token 을 **전부 무효화**한다. 관리자 4종 모두 호출할 수 있다.
                    성공 시 감사 로그(`ADMIN_LOGOUT`)를 남긴다.

                    **요청 필드** — 없음 (본문 없이 `Authorization` 헤더만 보낸다)

                    - 응답 `data` 는 `null` 이다.
                    - ⚠️ access token 은 만료(1시간) 전까지 유효하다 — 블랙리스트 미구현(`[VULN-LOC-2]` 범위).
                      따라서 로그아웃해도 **이미 발급된 access token 으로는 계속 호출된다.**
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Member member) {
        authService.logout(member);
        adminAuditLogService.record(AdminActivityType.ADMIN_LOGOUT, member.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
