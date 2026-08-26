package com.example.kidhackbackend.users.location.dto;

import com.example.kidhackbackend.users.location.entity.PlaceEvent;
import com.example.kidhackbackend.users.location.entity.PlaceEventType;

import java.time.LocalDateTime;

/** 진입/이탈/도착 이벤트 응답 DTO. */
public record PlaceEventResponse(
        Long id,
        Long childId,
        Long placeId,
        PlaceEventType eventType,
        LocalDateTime occurredAt
) {
    public static PlaceEventResponse from(PlaceEvent e) {
        return new PlaceEventResponse(
                e.getId(), e.getChildId(), e.getPlaceId(), e.getEventType(), e.getOccurredAt());
    }
}
