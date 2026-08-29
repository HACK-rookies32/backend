package com.example.kidhackbackend.users.appcontrol.dto;

import com.example.kidhackbackend.users.appcontrol.entity.ScreenTimeSetting;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자녀 하루 스크린타임 한도")
public record ScreenTimeResponse(
        @Schema(description = "대상 자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "하루 총 사용 한도(분). null 이면 제한 없음", example = "180",
                nullable = true) Integer dailyLimitMinutes
) {
    public static ScreenTimeResponse from(ScreenTimeSetting setting) {
        return new ScreenTimeResponse(setting.getChildId(), setting.getDailyLimitMinutes());
    }

    /** 설정한 적이 없는 자녀. 제한 없음으로 본다. */
    public static ScreenTimeResponse unlimited(Long childId) {
        return new ScreenTimeResponse(childId, null);
    }
}
