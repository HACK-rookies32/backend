package com.example.kidhackbackend.global.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "알림 목록 페이지")
public record NotificationPageResponse(
        @Schema(description = "알림 목록(최신순)") List<NotificationResponse> content,
        @Schema(description = "현재 페이지 번호(0부터)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "137") long totalElements,
        @Schema(description = "전체 페이지 수", example = "7") int totalPages
) {
}
