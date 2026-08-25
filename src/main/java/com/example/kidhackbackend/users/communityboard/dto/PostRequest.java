package com.example.kidhackbackend.users.communityboard.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PostRequest {
    private String title;
    private String content;
    private List<String> attachments;
    private List<String> links;
}