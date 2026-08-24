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

/**
 * 부모가 등록한 특정 공간(지오펜스/목적지) — 역할표 No.1·No.3.
 *
 * <p>중심 좌표와 반경(m)으로 원형 영역을 정의한다. 좌표는 거리 판정 계산에 쓰이므로 숫자로 저장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "place",
        indexes = @Index(name = "idx_place_child", columnList = "childId")
)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 공간이 속한 자녀. */
    @Column(nullable = false)
    private Long childId;

    /** 공간 이름(예: 학원). */
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    /** 반경(미터). */
    @Column(nullable = false)
    private double radiusMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlaceType type;

    public Place(Long childId, String name, double latitude, double longitude,
                 double radiusMeters, PlaceType type) {
        this.childId = childId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.type = type;
    }
}
