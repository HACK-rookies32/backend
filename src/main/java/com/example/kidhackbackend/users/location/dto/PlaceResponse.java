package com.example.kidhackbackend.users.location.dto;

import com.example.kidhackbackend.users.location.entity.Place;
import com.example.kidhackbackend.users.location.entity.PlaceType;

/** 등록 공간 응답 DTO. */
public record PlaceResponse(
        Long id,
        Long childId,
        String name,
        double latitude,
        double longitude,
        double radiusMeters,
        PlaceType type
) {
    public static PlaceResponse from(Place p) {
        return new PlaceResponse(
                p.getId(), p.getChildId(), p.getName(),
                p.getLatitude(), p.getLongitude(), p.getRadiusMeters(), p.getType());
    }
}
