package com.example.kidhackbackend.users.appcontrol.dto;

import com.example.kidhackbackend.users.appcontrol.entity.InstalledApp;

public record AppResponse(
        Long id,
        Long childId,
        String packageName,
        String appName
) {
    public static AppResponse from(InstalledApp app) {
        return new AppResponse(app.getId(), app.getChildId(), app.getPackageName(), app.getAppName());
    }
}