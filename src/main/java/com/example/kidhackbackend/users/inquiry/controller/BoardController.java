package com.example.kidhackbackend.users.inquiry.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.inquiry.dto.BoardDetailResponse;
import com.example.kidhackbackend.users.inquiry.dto.BoardRequest;
import com.example.kidhackbackend.users.inquiry.dto.BoardResponse;
import com.example.kidhackbackend.users.inquiry.service.BoardService;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Board", description = "부모 커뮤니티 게시판 API")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "게시글 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<BoardResponse>> createBoard(
            @AuthenticationPrincipal Member member,
            @RequestBody BoardRequest request) {
        return ResponseEntity.ok(ApiResponse.success(boardService.createBoard(member, request)));
    }

    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getAllBoards() {
        return ResponseEntity.ok(ApiResponse.success(boardService.getAllBoards()));
    }

    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardDetailResponse>> getBoardDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(boardService.getBoardDetail(id)));
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardResponse>> updateBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal Member member,
            @RequestBody BoardRequest request) {
        return ResponseEntity.ok(ApiResponse.success(boardService.updateBoard(id, member, request)));
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            @PathVariable Long id,
            @AuthenticationPrincipal Member member) {
        boardService.deleteBoard(id, member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}