package com.example.kidhackbackend.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 진입/이탈/도착 이벤트 이력 — 역할표 No.1·No.3.
 *
 * <p>상태 전환 판정과 도착 1회 발송(중복 방지)의 근거가 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "place_event",
        indexes = {
                @Index(name = "idx_event_child_place", columnList = "childId,placeId,occurredAt"),
                @Index(name = "idx_event_place_type", columnList = "placeId,eventType")
        }
)
public class PlaceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long childId;

    @Column(nullable = false)
    private Long placeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceEventType eventType;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    public PlaceEvent(Long childId, Long placeId, PlaceEventType eventType, LocalDateTime occurredAt) {
        this.childId = childId;
        this.placeId = placeId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
    }
}
