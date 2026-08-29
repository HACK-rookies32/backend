package com.example.kidhackbackend.users.appcontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;

@Schema(description = "자녀 하루 스크린타임 한도 설정 요청")
@Getter
public class ScreenTimeRequest {

    /**
     * 하루 총 사용 한도(분). {@code null} 이면 제한을 푼다.
     *
     * <p>0 은 "한도 0분"(= 사실상 사용 금지)이라 null 과 의미가 다르다. 상한은 하루(1440분).
     */
    @Schema(description = "하루 총 사용 한도(분). null 이면 제한 해제", example = "180",
            nullable = true)
    @Min(value = 0, message = "한도는 0분 이상이어야 합니다.")
    @Max(value = 1440, message = "한도는 하루(1440분)를 넘을 수 없습니다.")
    private Integer dailyLimitMinutes;
}
