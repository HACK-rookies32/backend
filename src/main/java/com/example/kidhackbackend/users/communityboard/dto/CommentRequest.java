package com.example.kidhackbackend.users.communityboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "댓글 작성 요청")
@Getter
public class CommentRequest {

    @Schema(description = "댓글 내용")
    private String content;
}
