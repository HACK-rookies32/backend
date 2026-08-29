package com.example.kidhackbackend.global.notice.dto;

import com.example.kidhackbackend.global.notice.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 사용자용 공지 <b>목록</b> 항목. 본문은 싣지 않는다(상세에서). 첨부·링크는 개수만.
 */
@Schema(description = "공지 목록 항목 (모바일용, 본문 없음)")
public record NoticeListItemResponse(
        @Schema(description = "공지 ID", example = "1") Long id,
        @Schema(description = "제목", example = "8월 정기 점검 안내") String title,
        @Schema(description = "작성자 회원 ID", example = "2") Long authorId,
        @Schema(description = "첨부 개수", example = "2") int attachmentCount,
        @Schema(description = "링크 개수", example = "1") int linkCount,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt
) {
    public static NoticeListItemResponse from(Notice notice) {
        return new NoticeListItemResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getAuthor().getId(),
                notice.getAttachments().size(),
                notice.getLinks().size(),
                notice.getCreatedAt(),
                notice.getUpdatedAt());
    }
}
