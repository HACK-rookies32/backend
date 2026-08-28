package com.example.kidhackbackend.admin.inquiry.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.inquiry.service.InquiryAdminService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.inquiry.dto.BoardDetailResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryAnswerRequest;
import com.example.kidhackbackend.users.inquiry.dto.InquiryAnswerResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryPageResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "40. 관리자 - 1:1 문의", description = "전체 문의 목록/상세 · 답변 등록/수정/삭제 (SUPER_ADMIN, INQUIRY_ADMIN)")
@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class InquiryAdminController {

    private final InquiryAdminService inquiryAdminService;
    private final AdminAuditLogService adminAuditLogService;

    private static final Set<Role> ALLOWED_ROLES = Set.of(Role.SUPER_ADMIN, Role.INQUIRY_ADMIN);

    private void requireInquiryAdmin(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (!ALLOWED_ROLES.contains(member.getRole())) {
            throw new IllegalArgumentException("문의 게시판 관리자만 접근할 수 있습니다.");
        }
    }

    @Operation(summary = "전체 문의 목록 조회 (페이징)")
    @GetMapping
    public ResponseEntity<ApiResponse<InquiryPageResponse>> getAllInquiries(
            @AuthenticationPrincipal Member member,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        requireInquiryAdmin(member);
        return ResponseEntity.ok(ApiResponse.success(inquiryAdminService.getAllInquiries(page, size)));
    }

    @Operation(summary = "문의 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardDetailResponse>> getInquiryDetail(
            @AuthenticationPrincipal Member member,
            @PathVariable Long id) {
        requireInquiryAdmin(member);
        return ResponseEntity.ok(ApiResponse.success(inquiryAdminService.getInquiryDetail(id)));
    }

    @Operation(summary = "답변 등록")
    @PostMapping("/{id}/answer")
    public ResponseEntity<ApiResponse<InquiryAnswerResponse>> createAnswer(
            @AuthenticationPrincipal Member member,
            @PathVariable Long id,
            @RequestBody InquiryAnswerRequest request) {
        requireInquiryAdmin(member);
        InquiryAnswerResponse response = inquiryAdminService.createAnswer(id, request);
        adminAuditLogService.record(member, AdminActivityType.INQUIRY_ANSWER_CREATE, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "답변 수정")
    @PutMapping("/{id}/answer")
    public ResponseEntity<ApiResponse<InquiryAnswerResponse>> updateAnswer(
            @AuthenticationPrincipal Member member,
            @PathVariable Long id,
            @RequestBody InquiryAnswerRequest request) {
        requireInquiryAdmin(member);
        InquiryAnswerResponse response = inquiryAdminService.updateAnswer(id, request);
        adminAuditLogService.record(member, AdminActivityType.INQUIRY_ANSWER_UPDATE, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "답변 삭제")
    @DeleteMapping("/{id}/answer")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(
            @AuthenticationPrincipal Member member,
            @PathVariable Long id) {
        requireInquiryAdmin(member);
        inquiryAdminService.deleteAnswer(id);
        adminAuditLogService.record(member, AdminActivityType.INQUIRY_ANSWER_DELETE, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}