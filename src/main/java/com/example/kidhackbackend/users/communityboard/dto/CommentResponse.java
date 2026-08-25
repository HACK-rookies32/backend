package com.example.kidhackbackend.users.communityboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CommentResponse {
    private Long commentId;
    private Long memberId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
}