package com.example.kidhackbackend.users.appcontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@Schema(description = "앱 통제 규칙 수정 요청 — 모든 필드 선택, null(생략)인 필드는 미변경")
public record RuleUpdateRequest(
        @Schema(description = "(선택) 일일 사용 제한(분, 0 이상)", example = "90") @Min(0) Integer dailyLimitMinutes,
        @Schema(description = "(선택) 규칙 활성화 여부", example = "false") Boolean enabled
) {}
