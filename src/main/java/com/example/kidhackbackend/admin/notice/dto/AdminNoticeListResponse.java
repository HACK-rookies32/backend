package com.example.kidhackbackend.admin.notice.dto;

import com.example.kidhackbackend.global.notice.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 관리자 공지 목록 행. 본문·첨부 원본은 싣지 않고 개수만 보여준다. */
@Schema(description = "관리자 공지 목록 행")
public record AdminNoticeListResponse(
        @Schema(description = "공지 ID", example = "1") Long id,
        @Schema(description = "제목") String title,
        @Schema(description = "작성자 이름(실명)", example = "관리자") String authorName,
        @Schema(description = "첨부 개수", example = "2") int attachmentCount,
        @Schema(description = "링크 개수", example = "1") int linkCount,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt
) {
    public static AdminNoticeListResponse from(Notice notice) {
        return new AdminNoticeListResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getAuthor().getUsername(),
                notice.getAttachments().size(),
                notice.getLinks().size(),
                notice.getCreatedAt(),
                notice.getUpdatedAt());
    }
}
