package com.example.kidhackbackend.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 자녀 앱이 업로드하는 현재 위치 요청.
 * 좌표는 스펙에 따라 문자열로 수신한다.
 */
public record TrackRequest(
        @NotNull Long childId,
        @NotBlank String latitude,
        @NotBlank String longitude
) {
}
