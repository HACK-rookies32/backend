package com.example.kidhackbackend.global.notice.dto;

import com.example.kidhackbackend.global.notice.entity.Notice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자(모바일)용 공지 응답.
 *
 * <p><b>필드 이름과 모양을 일부러 예전 그대로 둔다.</b> 관리자 웹만 새 계약
 * (noticeId/authorName/배열)으로 옮겼고, 모바일이 이미 붙여 둔 화면을 깨지 않기 위해서다.
 *
 * <p>엔티티는 첨부·링크를 여러 개 담게 바뀌었으므로 여기서는 첫 값만 내려준다.
 * 비어 있으면 {@code null} 이라 예전(값이 없을 때 null)과 동일하다.
 */
public record NoticeResponse(
        Long id, String title, String content, String attachmentUrl, String linkUrl,
        Long authorId, LocalDateTime createdAt, LocalDateTime updatedAt
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
