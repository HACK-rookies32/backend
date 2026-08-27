package com.example.kidhackbackend.users.notice.controller;

import com.example.kidhackbackend.global.notice.dto.NoticeResponse;
import com.example.kidhackbackend.global.notice.service.NoticeService;
import com.example.kidhackbackend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

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
}