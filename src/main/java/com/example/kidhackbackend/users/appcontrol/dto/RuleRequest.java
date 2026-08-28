package com.example.kidhackbackend.users.appcontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "앱 통제 규칙 추가 요청")
public record RuleRequest(
        @Schema(description = "자녀 회원 ID", example = "7") @NotNull Long childId,
        @Schema(description = "패키지명", example = "com.google.android.youtube") @NotNull String packageName,
        @Schema(description = "앱 이름", example = "YouTube") @NotNull String appName,
        @Schema(description = "일일 사용 제한(분)", example = "60") @NotNull Integer dailyLimitMinutes
) {}
