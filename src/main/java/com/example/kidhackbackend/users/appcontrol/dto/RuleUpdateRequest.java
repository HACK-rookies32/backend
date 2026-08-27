package com.example.kidhackbackend.users.appcontrol.dto;

public record RuleUpdateRequest(
        Integer dailyLimitMinutes,
        Boolean enabled
) {}