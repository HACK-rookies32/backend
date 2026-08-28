package com.example.kidhackbackend.users.inquiry.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.inquiry.dto.BoardDetailResponse;
import com.example.kidhackbackend.users.inquiry.dto.BoardRequest;
import com.example.kidhackbackend.users.inquiry.dto.BoardResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryPageResponse;
import com.example.kidhackbackend.users.inquiry.service.BoardService;
import com.example.kidhackbackend.users.inquiry.service.InquiryFileStorageService;
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

@Tag(name = "21. 1:1 문의 (회원)", description = "회원이 본인 문의 등록/조회/수정/삭제 · 첨부 업로드 — PARENT 전용")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final InquiryFileStorageService fileStorageService;

    private void requireParent(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.PARENT) {
            throw new IllegalArgumentException("부모 회원만 이용할 수 있는 기능입니다.");
        }
    }

    @Operation(summary = "게시글 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<BoardResponse>> createBoard(
            @AuthenticationPrincipal Member member,
            @RequestBody BoardRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(boardService.createBoard(member, request)));
    }

    @Operation(summary = "게시글 목록 조회 (본인 문의만, 페이징)")
    @GetMapping
    public ResponseEntity<ApiResponse<InquiryPageResponse>> getMyBoards(
            @AuthenticationPrincipal Member member,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(boardService.getMyBoards(member, page, size)));
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardDetailResponse>> getBoardDetail(
            @AuthenticationPrincipal Member member,
            @PathVariable Long id) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(boardService.getBoardDetail(id)));
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardResponse>> updateBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal Member member,
            @RequestBody BoardRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(boardService.updateBoard(id, member, request)));
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal Member member) {
        requireParent(member);
        boardService.deleteBoard(id, member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "첨부파일 업로드")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @AuthenticationPrincipal Member member,
            @RequestParam("file") MultipartFile file) {
        requireParent(member);
        String url = fileStorageService.store(file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }
}