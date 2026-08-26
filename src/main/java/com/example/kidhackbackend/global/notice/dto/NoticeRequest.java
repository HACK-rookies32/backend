package com.example.kidhackbackend.global.notice.dto;

import lombok.Getter;

@Getter
public class NoticeRequest {
    private String title;
    private String content;
    private String attachmentUrl;
    private String linkUrl;
}