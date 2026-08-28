package com.example.kidhackbackend.users.location.dto;

import com.example.kidhackbackend.users.location.entity.TrackSegment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 동선 세그먼트 응답 DTO. */
@Schema(description = "동선 세그먼트")
public record TrackSegmentResponse(
        @Schema(description = "세그먼트 ID", example = "2") Long id,
        @Schema(description = "자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "위도(문자열)", example = "37.5665") String latitude,
        @Schema(description = "경도(문자열)", example = "126.9780") String longitude,
        @Schema(description = "세그먼트 시작 시각") LocalDateTime segmentStart,
        @Schema(description = "세그먼트 종료 시각") LocalDateTime segmentEnd,
        @Schema(description = "갱신 시각") LocalDateTime updatedAt
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
