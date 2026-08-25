package com.example.kidhackbackend.users.inquiry.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BoardDetailResponse {
    private Long id;
    private Long memberId;
    private String memberNickname;
    private String title;
    private String content;
    private String attachmentUrl;
    private String linkUrl;
    private boolean answered;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private InquiryAnswerResponse answer; // 없으면 null
}