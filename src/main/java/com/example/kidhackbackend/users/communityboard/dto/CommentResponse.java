package com.example.kidhackbackend.users.communityboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "댓글")
@Getter
@AllArgsConstructor
public class CommentResponse {

    @Schema(description = "댓글 ID", example = "5")
    private Long commentId;

    @Schema(description = "작성자 회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "작성자 닉네임", example = "보라")
    private String nickname;

    @Schema(description = "댓글 내용")
    private String content;

    @Schema(description = "작성 시각")
    private LocalDateTime createdAt;
}
