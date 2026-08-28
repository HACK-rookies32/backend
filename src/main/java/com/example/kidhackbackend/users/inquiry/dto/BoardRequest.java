package com.example.kidhackbackend.users.inquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "1:1 문의 작성/수정 요청")
@Getter
public class BoardRequest {

    @Schema(description = "제목", example = "결제가 안 돼요")
    private String title;

    @Schema(description = "본문")
    private String content;

    @Schema(description = "첨부파일 URL (선택)")
    private String attachmentUrl;

    @Schema(description = "관련 링크 URL (선택)")
    private String linkUrl;
}
