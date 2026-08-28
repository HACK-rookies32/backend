package com.example.kidhackbackend.users.inquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "1:1 문의 답변")
@Getter
@AllArgsConstructor
public class InquiryAnswerResponse {

    @Schema(description = "답변 ID", example = "1")
    private Long id;

    @Schema(description = "답변 내용")
    private String content;

    @Schema(description = "작성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;
}
