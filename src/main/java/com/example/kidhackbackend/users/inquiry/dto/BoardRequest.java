package com.example.kidhackbackend.users.inquiry.dto;

import lombok.Getter;

@Getter
public class BoardRequest {
    private String title;
    private String content;
    private String attachmentUrl;
    private String linkUrl;
}