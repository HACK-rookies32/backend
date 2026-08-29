package com.example.kidhackbackend.users.notice.controller;

import com.example.kidhackbackend.global.notice.dto.NoticePageResponse;
import com.example.kidhackbackend.global.notice.dto.NoticeResponse;
import com.example.kidhackbackend.global.notice.service.NoticeService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "23. 공지사항", description = "공지 목록/상세 조회 — 열람은 인증 불필요 (비로그인 포함 누구나)")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지 목록 조회",
            description = """
                    최신순 페이징. 목록에는 본문이 실리지 않고 제목·첨부 개수만 내려간다(본문은 상세에서).

                    | 파라미터 | 필수 | 설명 |
                    |---|:---:|---|
                    | `keyword` | — | **제목** 부분 검색(대소문자 무시). 없으면 전체 |
                    | `page` / `size` | — | 페이징(기본 0 / 10) |
                    """)
    @GetMapping
    public ResponseEntity<ApiResponse<NoticePageResponse>> getNotices(
            @Parameter(description = "제목 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.getNotices(keyword, PageRequest.of(page, size))));
    }

    @Operation(summary = "공지 상세 조회",
            description = "본문·첨부·링크를 반환한다. 없는 id → 404.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeResponse>> getNotice(
            @Parameter(description = "공지 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotice(id)));
    }
}
