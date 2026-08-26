package com.example.kidhackbackend.users.filtering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 차단 규칙 등록 요청 (No.6). */
public record BlockRuleRequest(
        @NotNull Long childId,
        @NotBlank String pattern
) {
}
