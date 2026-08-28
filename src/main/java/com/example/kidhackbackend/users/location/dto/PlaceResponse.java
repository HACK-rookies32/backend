package com.example.kidhackbackend.users.location.dto;

import com.example.kidhackbackend.users.location.entity.Place;
import com.example.kidhackbackend.users.location.entity.PlaceType;
import io.swagger.v3.oas.annotations.media.Schema;

/** 등록 공간 응답 DTO. */
@Schema(description = "등록 공간(지오펜스/목적지)")
public record PlaceResponse(
        @Schema(description = "공간 ID", example = "1") Long id,
        @Schema(description = "자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "공간 이름", example = "학원") String name,
        @Schema(description = "위도", example = "37.5665") double latitude,
        @Schema(description = "경도", example = "126.978") double longitude,
        @Schema(description = "반경(m)", example = "200.0") double radiusMeters,
        @Schema(description = "공간 유형", example = "GEOFENCE") PlaceType type
) {
    public static PlaceResponse from(Place p) {
        return new PlaceResponse(
                p.getId(), p.getChildId(), p.getName(),
                p.getLatitude(), p.getLongitude(), p.getRadiusMeters(), p.getType());
    }
}
