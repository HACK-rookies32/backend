package com.example.kidhackbackend.admin.community.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.community.dto.AdminCommentResponse;
import com.example.kidhackbackend.admin.community.dto.AdminPostDetailResponse;
import com.example.kidhackbackend.admin.community.dto.AdminPostResponse;
import com.example.kidhackbackend.admin.community.dto.MemberBanRequest;
import com.example.kidhackbackend.admin.community.dto.MemberBanResponse;
import com.example.kidhackbackend.admin.community.service.AdminCommunityService;
import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
import com.example.kidhackbackend.global.exception.InvalidCredentialException;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "36. 관리자 - 커뮤니티", description = "관리자 커뮤니티 게시글 목록/상세 조회 · 삭제")
@RestController
@RequiredArgsConstructor
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;
    private final AdminAuditLogService adminAuditLogService;

    private void requireParent(Member member) {
        if (member == null) {
            throw new InvalidCredentialException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.PARENT) {
            throw new AdminAccessDeniedException("부모 회원만 이용할 수 있는 기능입니다.");
        }
    }

    // --- 사용자 신고 API (API 명세서에서는 숨김 — 코드는 유지) ---

    @Hidden
    @Operation(summary = "게시글 신고")
    @PostMapping("/api/community/posts/{postId}/report")
    public ResponseEntity<ApiResponse<String>> reportPost(
            @AuthenticationPrincipal Member member,
            @PathVariable Long postId) {
        requireParent(member);
        adminCommunityService.reportPost(postId, member);
        return ResponseEntity.ok(ApiResponse.success("게시글 신고가 완료되었습니다."));
    }

    @Hidden
    @Operation(summary = "댓글 신고")
    @PostMapping("/api/community/posts/{postId}/comments/{commentId}/report")
    public ResponseEntity<ApiResponse<String>> reportComment(
            @AuthenticationPrincipal Member member,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        requireParent(member);
        adminCommunityService.reportComment(postId, commentId, member);
        return ResponseEntity.ok(ApiResponse.success("댓글 신고가 완료되었습니다."));
    }

    // --- 관리자 커뮤니티 관리 API ---

    @Operation(summary = "전체 게시글 목록 페이징 조회 (관리자)")
    @GetMapping("/api/admin/community/posts")
    public ResponseEntity<ApiResponse<Page<AdminPostResponse>>> getAllPosts(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminCommunityService.getAllPosts(pageable)));
    }

    @Operation(summary = "게시글 상세 조회 (관리자)")
    @GetMapping("/api/admin/community/posts/{id}")
    public ResponseEntity<ApiResponse<AdminPostDetailResponse>> getPostDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminCommunityService.getPostDetail(id)));
    }

    @Hidden
    @Operation(summary = "신고된 게시글 목록 페이징 조회 (관리자)")
    @GetMapping("/api/admin/community/reports/posts")
    public ResponseEntity<ApiResponse<Page<AdminPostResponse>>> getReportedPosts(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminCommunityService.getReportedPosts(pageable)));
    }

    @Hidden
    @Operation(summary = "신고된 댓글 목록 페이징 조회 (관리자)")
    @GetMapping("/api/admin/community/reports/comments")
    public ResponseEntity<ApiResponse<Page<AdminCommentResponse>>> getReportedComments(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminCommunityService.getReportedComments(pageable)));
    }

    @Hidden
    @Operation(summary = "신고된 게시글 상세 조회 (관리자)")
    @GetMapping("/api/admin/community/reports/posts/{id}")
    public ResponseEntity<ApiResponse<AdminPostDetailResponse>> getReportedPostDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminCommunityService.getPostDetail(id)));
    }

    @Hidden
    @Operation(summary = "신고된 댓글 상세 조회 (관리자)")
    @GetMapping("/api/admin/community/reports/comments/{id}")
    public ResponseEntity<ApiResponse<AdminCommentResponse>> getReportedCommentDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminCommunityService.getReportedCommentDetail(id)));
    }

    @Operation(summary = "관리자 커뮤니티 게시글 삭제")
    @DeleteMapping("/api/admin/community/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal Member member,
            @PathVariable("postId") Long postId) {
        adminCommunityService.deletePost(postId);
        adminAuditLogService.record(AdminActivityType.POST_DELETE, postId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }


    @Hidden
    @Operation(summary = "커뮤니티 사용자 밴/해제 (관리자)",
            description = "durationDays 를 0 으로 보내면 밴이 해제됩니다.")
    @PostMapping("/api/admin/community/members/ban")
    public ResponseEntity<ApiResponse<MemberBanResponse>> banMember(
            @Valid @RequestBody MemberBanRequest request) {
        MemberBanResponse response = adminCommunityService.banMember(request);
        adminAuditLogService.record(AdminActivityType.MEMBER_BAN, response.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

}
