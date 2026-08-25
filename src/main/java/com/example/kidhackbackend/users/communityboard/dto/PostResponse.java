package com.example.kidhackbackend.users.communityboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostResponse {
    private Long postId;
    private String title;
    private String authorNickname;
    private long likeCount;
    private LocalDateTime createdAt;
}