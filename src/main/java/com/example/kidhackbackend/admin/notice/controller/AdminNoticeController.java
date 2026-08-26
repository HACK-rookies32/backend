package com.example.kidhackbackend.admin.notice.controller;

import com.example.kidhackbackend.global.notice.dto.NoticeRequest;
import com.example.kidhackbackend.global.notice.dto.NoticeResponse;
import com.example.kidhackbackend.global.notice.service.NoticeService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NoticeResponse>>> getNotices(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotices(PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeResponse>> getNotice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotice(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @AuthenticationPrincipal Member member, @RequestBody NoticeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.createNotice(member.getId(), request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @PathVariable Long id, @RequestBody NoticeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.updateNotice(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}