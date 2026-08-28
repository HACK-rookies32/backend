package com.example.kidhackbackend.users.location.dto;

import com.example.kidhackbackend.users.location.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 지오펜스/목적지 등록 요청 (No.1·No.3). 모든 필드 필수. */
@Schema(description = "지오펜스/목적지 등록 요청")
public record PlaceRequest(
        @Schema(description = "(필수) 자녀 회원 ID", example = "7", requiredMode = RequiredMode.REQUIRED)
        @NotNull Long childId,
        @Schema(description = "(필수) 공간 이름", example = "학원", requiredMode = RequiredMode.REQUIRED)
        @NotBlank String name,
        @Schema(description = "(필수) 위도(-90~90)", example = "37.5665", requiredMode = RequiredMode.REQUIRED)
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @Schema(description = "(필수) 경도(-180~180)", example = "126.9780", requiredMode = RequiredMode.REQUIRED)
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Schema(description = "(필수) 반경(m, 양수)", example = "200", requiredMode = RequiredMode.REQUIRED)
        @NotNull @Positive Double radiusMeters,
        @Schema(description = "(필수) 공간 유형: GEOFENCE(진입/이탈) / DESTINATION(도착)", example = "GEOFENCE",
                requiredMode = RequiredMode.REQUIRED)
        @NotNull PlaceType type
) {
}
