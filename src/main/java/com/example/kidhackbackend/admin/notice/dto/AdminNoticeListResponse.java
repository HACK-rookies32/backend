package com.example.kidhackbackend.admin.notice.dto;

import com.example.kidhackbackend.global.notice.entity.Notice;

import java.time.LocalDateTime;

/**
 * 관리자 공지 목록 행.
 *
 * <p>본문과 첨부·링크 원본은 싣지 않는다. 목록 화면은 <b>개수만</b> 보여주고, 실물은
 * 상세에서 받아 간다. 목록마다 CLOB 본문을 실어 나를 이유가 없다.
 */
public record AdminNoticeListResponse(
        Long noticeId,
        String title,
        String authorName,
        int attachmentCount,
        int linkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
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
