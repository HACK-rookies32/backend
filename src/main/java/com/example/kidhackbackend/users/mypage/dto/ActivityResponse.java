package com.example.kidhackbackend.users.mypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "활동 내역 항목")
@Getter
@AllArgsConstructor
public class ActivityResponse {

    @Schema(description = "활동 항목 ID(게시글/댓글/좋아요 각각의 PK)", example = "15")
    private Long id;

    @Schema(description = "활동 종류: POST / COMMENT / LIKE", example = "POST")
    private String type;

    @Schema(description = "관련 게시글 ID", example = "15")
    private Long postId;

    @Schema(description = "미리보기 — POST=제목, COMMENT=댓글 내용, LIKE=null", example = "오늘 놀이터에서 있었던 일")
    private String preview;

    @Schema(description = "활동 발생 시각")
    private LocalDateTime createdAt;
}
