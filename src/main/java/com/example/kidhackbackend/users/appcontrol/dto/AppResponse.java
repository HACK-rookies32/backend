package com.example.kidhackbackend.users.appcontrol.dto;

import com.example.kidhackbackend.users.appcontrol.entity.InstalledApp;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자녀 기기에 설치된 앱")
public record AppResponse(
        @Schema(description = "레코드 ID", example = "1") Long id,
        @Schema(description = "자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "패키지명", example = "com.google.android.youtube") String packageName,
        @Schema(description = "앱 이름", example = "YouTube") String appName
) {
    public static AppResponse from(InstalledApp app) {
        return new AppResponse(app.getId(), app.getChildId(), app.getPackageName(), app.getAppName());
    }
}
