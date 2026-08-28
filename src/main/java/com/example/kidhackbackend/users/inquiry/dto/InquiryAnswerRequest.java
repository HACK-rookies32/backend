package com.example.kidhackbackend.users.inquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "1:1 문의 답변 등록/수정 요청 (관리자)")
@Getter
public class InquiryAnswerRequest {

    @Schema(description = "답변 내용")
    private String content;
}
