package com.example.kidhackbackend.users.filtering.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 차단 규칙 등록 요청 (No.6). 모든 필드 필수. */
@Schema(description = "차단 규칙 등록 요청")
public record BlockRuleRequest(
        @Schema(description = "(필수) 자녀 회원 ID", example = "7", requiredMode = RequiredMode.REQUIRED)
        @NotNull Long childId,
        @Schema(description = "(필수) 차단 패턴(도메인/키워드 등)", example = "badsite.com",
                requiredMode = RequiredMode.REQUIRED)
        @NotBlank String pattern
) {
}
