package com.example.kidhackbackend.users.appcontrol.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SyncRequest(
        @NotNull Long childId,
        @NotEmpty @Valid List<AppItem> apps
) {
    public record AppItem(
            @NotNull String packageName,
            @NotNull String appName
    ) {}
}