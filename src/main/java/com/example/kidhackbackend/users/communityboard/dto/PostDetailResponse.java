package com.example.kidhackbackend.users.communityboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class PostDetailResponse {
    private Long postId;
    private Long authorId;
    private String authorNickname;
    private String title;
    private String content;
    private List<String> attachments;
    private List<String> links;
    private long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}