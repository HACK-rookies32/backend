package com.example.kidhackbackend.admin.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "관리자 공지 목록 페이지")
public record AdminNoticePageResponse(
        @Schema(description = "공지 목록") List<AdminNoticeListResponse> content,
        @Schema(description = "현재 페이지 번호(0부터)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "10") int size,
        @Schema(description = "전체 항목 수", example = "24") long totalElements,
        @Schema(description = "전체 페이지 수", example = "3") int totalPages
) {
}
