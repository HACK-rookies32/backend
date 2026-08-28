package com.example.kidhackbackend.admin.member.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.member.dto.AdminMemberResponse;
import com.example.kidhackbackend.admin.member.dto.AdminRoleUpdateRequest;
import com.example.kidhackbackend.admin.member.service.AdminMemberService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "31. 관리자 - 관리자 계정", description = "관리자 계정 목록 조회 · 역할 변경 (SUPER_ADMIN)")
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;
    private final AdminAuditLogService adminAuditLogService;

    @Operation(summary = "관리자 계정 목록 조회", description = "관리자 역할 회원 목록을 반환한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminMemberResponse>>> getAdminList() {
        List<AdminMemberResponse> admins = adminMemberService.getAdminList();
        return ResponseEntity.ok(ApiResponse.success(admins));
    }

    @Operation(summary = "관리자 역할 변경",
            description = """
                    대상 **관리자**의 역할을 변경한다. 감사 로그(`MEMBER_ROLE_UPDATE`)를 남긴다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    | --- | --- | --- |
                    | `role` | ✅ | `SUPER_ADMIN` / `GENERAL_ADMIN` / `COMMUNITY_ADMIN` / `INQUIRY_ADMIN` |

                    - 경로의 `id` 는 **관리자 계정**이어야 한다. 일반 회원(PARENT/CHILD)이면 `400 INVALID_PARAMETER`.
                      (일반 회원을 관리자로 승격시키는 용도가 아니다)
                    - 존재하지 않는 계정: `404 NOT_FOUND`.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "역할 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "대상 계정이 관리자가 아님",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "data": null,
                             "error": {"code": "INVALID_PARAMETER", "message": "대상 계정이 관리자가 아닙니다."}}"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 계정",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "data": null,
                             "error": {"code": "NOT_FOUND", "message": "존재하지 않는 계정입니다."}}""")))
    })
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> updateAdminRole(
            @Parameter(description = "대상 관리자 회원 ID") @PathVariable Long id,
            @Valid @RequestBody AdminRoleUpdateRequest request) {
        AdminMemberResponse response = adminMemberService.updateAdminRole(id, request.getRole());
        adminAuditLogService.record(AdminActivityType.MEMBER_ROLE_UPDATE, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}