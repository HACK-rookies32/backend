package com.example.kidhackbackend.users.appcontrol.dto;

import com.example.kidhackbackend.users.appcontrol.entity.AppControlRule;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 통제 규칙")
public record RuleResponse(
        @Schema(description = "규칙 ID", example = "1") Long id,
        @Schema(description = "자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "패키지명", example = "com.google.android.youtube") String packageName,
        @Schema(description = "앱 이름", example = "YouTube") String appName,
        @Schema(description = "일일 사용 제한(분)", example = "60") int dailyLimitMinutes,
        @Schema(description = "규칙 활성화 여부", example = "true") boolean enabled
) {
    public static RuleResponse from(AppControlRule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getChildId(),
                rule.getPackageName(),
                rule.getAppName(),
                rule.getDailyLimitMinutes(),
                rule.isEnabled()
        );
    }
}
