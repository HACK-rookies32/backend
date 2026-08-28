package com.example.kidhackbackend.users.notice.controller;

import com.example.kidhackbackend.global.notice.dto.NoticeResponse;
import com.example.kidhackbackend.global.notice.service.NoticeService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "23. 공지사항", description = "공지 목록/상세 조회 (열람은 인증 불필요)")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지 목록 조회", description = "페이징 조회.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NoticeResponse>>> getNotices(
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotices(PageRequest.of(page, size))));
    }

    @Operation(summary = "공지 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeResponse>> getNotice(
            @Parameter(description = "공지 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotice(id)));
    }
}
