package com.example.kidhackbackend.users.location.dto;

import com.example.kidhackbackend.users.location.entity.PlaceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 지오펜스/목적지 등록 요청 (No.1·No.3). */
public record PlaceRequest(
        @NotNull Long childId,
        @NotBlank String name,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @Positive Double radiusMeters,
        @NotNull PlaceType type
) {
}
