package com.example.kidhackbackend.users.inquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Schema(description = "1:1 문의 답변 등록/수정 요청 (관리자)")
@Getter
public class InquiryAnswerRequest {

    @Schema(description = "답변 내용")
    @NotBlank(message = "답변 내용은 필수입니다.")
    private String content;
}
