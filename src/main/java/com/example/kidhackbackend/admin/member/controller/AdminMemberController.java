package com.example.kidhackbackend.admin.member.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.member.dto.AdminMemberResponse;
import com.example.kidhackbackend.admin.member.dto.AdminRoleUpdateRequest;
import com.example.kidhackbackend.admin.member.service.AdminMemberService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "관리자 역할 변경", description = "대상 관리자의 역할을 변경한다. 감사 로그(MEMBER_ROLE_UPDATE)를 남긴다.")
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> updateAdminRole(
            @Parameter(description = "대상 관리자 회원 ID") @PathVariable Long id,
            @Valid @RequestBody AdminRoleUpdateRequest request) {
        AdminMemberResponse response = adminMemberService.updateAdminRole(id, request.getRole());
        adminAuditLogService.record(AdminActivityType.MEMBER_ROLE_UPDATE, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}