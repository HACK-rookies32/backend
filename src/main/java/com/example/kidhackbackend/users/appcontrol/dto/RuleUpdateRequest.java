package com.example.kidhackbackend.users.appcontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 통제 규칙 수정 요청 — null 인 필드는 미변경")
public record RuleUpdateRequest(
        @Schema(description = "일일 사용 제한(분)", example = "90") Integer dailyLimitMinutes,
        @Schema(description = "규칙 활성화 여부", example = "false") Boolean enabled
) {}
