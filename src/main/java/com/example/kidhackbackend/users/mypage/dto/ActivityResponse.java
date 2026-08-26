package com.example.kidhackbackend.users.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ActivityResponse {
    private Long id;
    private String type;       // POST / LIKE / COMMENT
    private Long postId;       // 관련 게시글 ID
    private String preview;    // POST=제목, COMMENT=댓글 내용, LIKE=null
    private LocalDateTime createdAt;
}