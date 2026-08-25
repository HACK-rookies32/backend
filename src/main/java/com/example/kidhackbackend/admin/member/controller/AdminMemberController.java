package com.example.kidhackbackend.admin.member.controller;

import com.example.kidhackbackend.admin.member.dto.AdminMemberResponse;
import com.example.kidhackbackend.admin.member.dto.AdminRoleUpdateRequest;
import com.example.kidhackbackend.admin.member.service.AdminMemberService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Member")
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminMemberResponse>>> getAdminList() {
        List<AdminMemberResponse> admins = adminMemberService.getAdminList();
        return ResponseEntity.ok(ApiResponse.success(admins));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> updateAdminRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminRoleUpdateRequest request) {
        AdminMemberResponse response = adminMemberService.updateAdminRole(id, request.getRole());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}