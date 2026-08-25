package com.example.kidhackbackend.location.dto;

import com.example.kidhackbackend.location.entity.TrackSegment;

import java.time.LocalDateTime;

/** 동선 세그먼트 응답 DTO. */
public record TrackSegmentResponse(
        Long id,
        Long childId,
        String latitude,
        String longitude,
        LocalDateTime segmentStart,
        LocalDateTime segmentEnd,
        LocalDateTime updatedAt
) {
    public static TrackSegmentResponse from(TrackSegment s) {
        return new TrackSegmentResponse(
                s.getId(),
                s.getChildId(),
                s.getLatitude(),
                s.getLongitude(),
                s.getSegmentStart(),
                s.getSegmentEnd(),
                s.getUpdatedAt()
        );
    }
}
