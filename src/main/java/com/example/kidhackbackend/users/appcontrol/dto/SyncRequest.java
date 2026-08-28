package com.example.kidhackbackend.users.appcontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "설치 앱 목록 동기화 요청")
public record SyncRequest(
        @Schema(description = "자녀 회원 ID", example = "7") @NotNull Long childId,
        @Schema(description = "설치 앱 목록(1건 이상)") @NotEmpty @Valid List<AppItem> apps
) {
    @Schema(description = "설치 앱 1건")
    public record AppItem(
            @Schema(description = "패키지명", example = "com.google.android.youtube") @NotNull String packageName,
            @Schema(description = "앱 이름", example = "YouTube") @NotNull String appName
    ) {}
}
