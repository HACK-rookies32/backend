package com.example.kidhackbackend.users.appcontrol.dto;

import com.example.kidhackbackend.users.appcontrol.entity.AppControlRule;

public record RuleResponse(
        Long id,
        Long childId,
        String packageName,
        String appName,
        int dailyLimitMinutes,
        boolean enabled
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