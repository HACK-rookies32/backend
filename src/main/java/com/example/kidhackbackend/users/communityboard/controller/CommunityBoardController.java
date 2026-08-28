package com.example.kidhackbackend.users.communityboard.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.communityboard.dto.*;
import com.example.kidhackbackend.users.communityboard.service.CommunityBoardService;
import com.example.kidhackbackend.users.communityboard.service.FileStorageService;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "20. 커뮤니티 게시판", description = "부모 커뮤니티 게시글/댓글/좋아요/첨부 — PARENT 전용")
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityBoardController {

    private final CommunityBoardService communityBoardService;
    private final FileStorageService fileStorageService;

    private void requireParent(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.PARENT) {
            throw new IllegalArgumentException("부모 회원만 이용할 수 있는 기능입니다.");
        }
    }

    @Operation(summary = "게시글 목록/검색")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PostPageResponse>> getPosts(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(communityBoardService.getPosts(keyword, page, size)));
    }

    @Operation(summary = "게시글 작성")
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal Member member,
            @RequestBody PostRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(communityBoardService.createPost(member, request)));
    }

    @Operation(summary = "게시글 상세")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @AuthenticationPrincipal Member member,
            @PathVariable Long postId) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(communityBoardService.getPostDetail(postId)));
    }

    @Operation(summary = "게시글 수정")
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @AuthenticationPrincipal Member member,
            @PathVariable Long postId,
            @RequestBody PostRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(communityBoardService.updatePost(postId, member, request)));
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal Member member,
            @PathVariable Long postId) {
        requireParent(member);
        communityBoardService.deletePost(postId, member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "댓글 작성")
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal Member member,
            @PathVariable Long postId,
            @RequestBody CommentRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                communityBoardService.createComment(postId, member, request)));
    }

    @Operation(summary = "좋아요 토글")
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(
            @AuthenticationPrincipal Member member,
            @PathVariable Long postId) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(communityBoardService.toggleLike(postId, member)));
    }

    @Operation(summary = "첨부파일 업로드")
    @PostMapping("/attachments")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAttachment(
            @AuthenticationPrincipal Member member,
            @RequestParam("file") MultipartFile file) {
        requireParent(member);
        String url = fileStorageService.store(file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }
}