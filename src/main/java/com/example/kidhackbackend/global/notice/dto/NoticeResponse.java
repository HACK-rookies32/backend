package com.example.kidhackbackend.global.notice.dto;

import com.example.kidhackbackend.global.notice.entity.Notice;

import java.time.LocalDateTime;

public record NoticeResponse(
        Long id, String title, String content, String attachmentUrl, String linkUrl,
        Long authorId, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(), notice.getTitle(), notice.getContent(),
                notice.getAttachmentUrl(), notice.getLinkUrl(), notice.getAuthorId(),
                notice.getCreatedAt(), notice.getUpdatedAt());
    }
}