package com.example.kidhackbackend.users.location.dto;

import com.example.kidhackbackend.users.location.entity.TrackSegment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 동선 조회 응답 — 프론트가 하루치 위·경도 리스트를 받아 점을 순서대로 이어 동선을 그리는 용도.
 * points 는 시간 오름차순이다.
 */
@Schema(description = "동선(위·경도 포인트 리스트, 시간 오름차순)")
public record TrackResponse(
        @Schema(description = "좌표 포인트 목록(시간 오름차순)") List<Point> points,
        @Schema(description = "포인트 개수", example = "288") int count
) {
    @Schema(description = "동선 포인트")
    public record Point(
            @Schema(description = "위도", example = "37.5665") double lat,
            @Schema(description = "경도", example = "126.978") double lng,
            @Schema(description = "해당 좌표가 찍힌 시각", example = "2026-08-27T12:05:00") LocalDateTime at
    ) {
    }

    public static TrackResponse of(List<TrackSegment> segments) {
        List<Point> points = segments.stream()
                .map(s -> new Point(
                        Double.parseDouble(s.getLatitude()),
                        Double.parseDouble(s.getLongitude()),
                        s.getUpdatedAt()))
                .toList();
        return new TrackResponse(points, points.size());
    }
}
