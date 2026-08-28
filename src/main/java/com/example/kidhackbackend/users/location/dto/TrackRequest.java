package com.example.kidhackbackend.users.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 자녀 앱이 업로드하는 현재 위치 요청.
 * 좌표는 스펙에 따라 문자열로 수신한다.
 */
@Schema(description = "위치 업로드 요청 (좌표는 문자열)")
public record TrackRequest(
        @Schema(description = "자녀 회원 ID", example = "7") @NotNull Long childId,
        @Schema(description = "위도(문자열)", example = "37.5665") @NotBlank String latitude,
        @Schema(description = "경도(문자열)", example = "126.9780") @NotBlank String longitude
) {
}
