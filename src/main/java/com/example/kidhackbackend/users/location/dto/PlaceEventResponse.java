package com.example.kidhackbackend.users.location.dto;

import com.example.kidhackbackend.users.location.entity.PlaceEvent;
import com.example.kidhackbackend.users.location.entity.PlaceEventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 진입/이탈/도착 이벤트 응답 DTO. */
@Schema(description = "장소 진입/이탈/도착 이벤트")
public record PlaceEventResponse(
        @Schema(description = "이벤트 ID", example = "1") Long id,
        @Schema(description = "자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "관련 등록 공간 ID", example = "1") Long placeId,
        @Schema(description = "이벤트 유형", example = "ENTER") PlaceEventType eventType,
        @Schema(description = "발생 시각") LocalDateTime occurredAt
) {
    public static PlaceEventResponse from(PlaceEvent e) {
        return new PlaceEventResponse(
                e.getId(), e.getChildId(), e.getPlaceId(), e.getEventType(), e.getOccurredAt());
    }
}
