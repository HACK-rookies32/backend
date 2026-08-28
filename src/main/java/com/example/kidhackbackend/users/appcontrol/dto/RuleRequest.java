package com.example.kidhackbackend.users.appcontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "앱 통제 규칙 추가 요청 — 모든 필드 필수")
public record RuleRequest(
        @Schema(description = "(필수) 자녀 회원 ID", example = "7", requiredMode = RequiredMode.REQUIRED)
        @NotNull Long childId,
        @Schema(description = "(필수) 패키지명", example = "com.google.android.youtube",
                requiredMode = RequiredMode.REQUIRED)
        @NotNull String packageName,
        @Schema(description = "(필수) 앱 이름", example = "YouTube", requiredMode = RequiredMode.REQUIRED)
        @NotNull String appName,
        @Schema(description = "(필수) 일일 사용 제한(분, 0 이상. 0=완전 차단)", example = "60",
                requiredMode = RequiredMode.REQUIRED)
        @NotNull @Min(0) Integer dailyLimitMinutes
) {}
