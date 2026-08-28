package com.example.kidhackbackend.users.inquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "1:1 문의 작성/수정 요청")
@Getter
public class InquiryRequest {

    @Schema(description = "제목", example = "결제가 안 돼요",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "본문", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "첨부 공개 경로 목록(POST /api/inquiries/upload 응답의 url). 선택",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<String> attachments;

    @Schema(description = "관련 링크 URL. 선택", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String linkUrl;
}
