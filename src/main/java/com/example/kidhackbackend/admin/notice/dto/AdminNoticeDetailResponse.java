package com.example.kidhackbackend.admin.notice.dto;

import com.example.kidhackbackend.global.notice.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** 관리자 공지 상세. 작성·수정 응답도 이 모양이다. */
@Schema(description = "관리자 공지 상세")
public record AdminNoticeDetailResponse(
        @Schema(description = "공지 ID", example = "1") Long noticeId,
        @Schema(description = "제목") String title,
        @Schema(description = "본문") String content,
        @Schema(description = "작성자 이름(실명)", example = "관리자") String authorName,
        @Schema(description = "첨부파일 URL 목록") List<String> attachments,
        @Schema(description = "관련 링크 목록") List<String> links,
        @Schema(description = "첨부 개수", example = "2") int attachmentCount,
        @Schema(description = "링크 개수", example = "1") int linkCount,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt
) {
    public static AdminNoticeDetailResponse from(Notice notice) {
        List<String> attachments = List.copyOf(notice.getAttachments());
        List<String> links = List.copyOf(notice.getLinks());

        return new AdminNoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getAuthor().getUsername(),
                attachments,
                links,
                attachments.size(),
                links.size(),
                notice.getCreatedAt(),
                notice.getUpdatedAt());
    }
}
