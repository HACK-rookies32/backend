package com.example.kidhackbackend.global.notice.dto;

import com.example.kidhackbackend.global.notice.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자(모바일)용 공지 상세 응답. 첨부·링크는 커뮤니티/문의와 동일하게 배열로 내려준다.
 */
@Schema(description = "공지 상세 (모바일용)")
public record NoticeResponse(
        @Schema(description = "공지 ID", example = "1") Long id,
        @Schema(description = "제목", example = "8월 정기 점검 안내") String title,
        @Schema(description = "본문") String content,
        @Schema(description = "첨부 공개 경로 목록 (없으면 빈 배열)") List<String> attachments,
        @Schema(description = "관련 링크 목록 (없으면 빈 배열)") List<String> links,
        @Schema(description = "작성자 회원 ID", example = "2") Long authorId,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt
) {
    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(), notice.getTitle(), notice.getContent(),
                List.copyOf(notice.getAttachments()),
                List.copyOf(notice.getLinks()),
                notice.getAuthor().getId(),
                notice.getCreatedAt(), notice.getUpdatedAt());
    }
}
