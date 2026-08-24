package com.example.kidhackbackend.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 실시간 동선 트래킹 세그먼트 (역할표 No.2).
 *
 * <p>스펙: "위/경도 문자열, 5분 단위 시작·종료 세그먼트로 기록, 3일치 보관 후 롤링 삭제(배치)".
 * 좌표는 스펙 명시대로 문자열(String)로 저장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "track_segment",
        indexes = {
                @Index(name = "idx_track_child_window", columnList = "childId,segmentStart"),
                @Index(name = "idx_track_created_at", columnList = "createdAt")
        }
)
public class TrackSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 자녀(기기) 식별자. */
    @Column(nullable = false)
    private Long childId;

    /** 위도 — 스펙에 따라 문자열로 저장. */
    @Column(nullable = false)
    private String latitude;

    /** 경도 — 스펙에 따라 문자열로 저장. */
    @Column(nullable = false)
    private String longitude;

    /** 5분 창 시작 시각(정렬된 경계). */
    @Column(nullable = false)
    private LocalDateTime segmentStart;

    /** 5분 창 종료 시각. */
    @Column(nullable = false)
    private LocalDateTime segmentEnd;

    /** 마지막 좌표가 갱신된 시각. */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 적재 시각 — 3일 롤링 삭제의 기준. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public TrackSegment(Long childId, String latitude, String longitude,
                        LocalDateTime segmentStart, LocalDateTime segmentEnd, LocalDateTime now) {
        this.childId = childId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.segmentStart = segmentStart;
        this.segmentEnd = segmentEnd;
        this.updatedAt = now;
        this.createdAt = now;
    }

    /** 같은 5분 창 안에서 새 좌표가 들어오면 최신 좌표로 갱신한다. */
    public void updatePosition(String latitude, String longitude, LocalDateTime now) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = now;
    }
}
