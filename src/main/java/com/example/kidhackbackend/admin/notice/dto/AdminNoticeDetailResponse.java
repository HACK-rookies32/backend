package com.example.kidhackbackend.admin.notice.dto;

import com.example.kidhackbackend.global.notice.entity.Notice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 공지 상세.
 *
 * <p>목록 행이 가진 것 전부에 본문과 첨부·링크 원본을 더한다. 작성·수정 응답도 이 모양이다.
 */
public record AdminNoticeDetailResponse(
        Long noticeId,
        String title,
        String content,
        String authorName,
        List<String> attachments,
        List<String> links,
        int attachmentCount,
        int linkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
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
