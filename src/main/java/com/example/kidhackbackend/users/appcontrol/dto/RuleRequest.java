package com.example.kidhackbackend.users.appcontrol.dto;

import jakarta.validation.constraints.NotNull;

public record RuleRequest(
        @NotNull Long childId,
        @NotNull String packageName,
        @NotNull String appName,
        @NotNull Integer dailyLimitMinutes
) {}