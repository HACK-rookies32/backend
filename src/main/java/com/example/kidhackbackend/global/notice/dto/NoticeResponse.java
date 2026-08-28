package com.example.kidhackbackend.global.notice.dto;

import com.example.kidhackbackend.global.notice.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자(모바일)용 공지 응답. 첨부·링크는 첫 값만 내려준다(구계약 유지).
 */
@Schema(description = "공지 (모바일용)")
public record NoticeResponse(
        @Schema(description = "공지 ID", example = "1") Long id,
        @Schema(description = "제목", example = "8월 정기 점검 안내") String title,
        @Schema(description = "본문") String content,
        @Schema(description = "첨부파일 URL (없으면 null — 첫 값만)") String attachmentUrl,
        @Schema(description = "관련 링크 URL (없으면 null — 첫 값만)") String linkUrl,
        @Schema(description = "작성자 회원 ID", example = "2") Long authorId,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt
) {
    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(), notice.getTitle(), notice.getContent(),
                first(notice.getAttachments()), first(notice.getLinks()),
                notice.getAuthor().getId(),
                notice.getCreatedAt(), notice.getUpdatedAt());
    }

    private static String first(List<String> urls) {
        return (urls == null || urls.isEmpty()) ? null : urls.get(0);
    }
}
