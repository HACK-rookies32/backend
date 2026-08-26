package com.example.kidhackbackend.users.location.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자녀별 실시간 위치 추적 설정.
 *
 * <p>{@code realtimeEnabled = false}(기본)면 5분 단위 세그먼트로 저장하고,
 * {@code true}면 업로드되는 좌표를 매번 실시간 포인트로 저장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tracking_setting")
public class TrackingSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 자녀 (자녀당 1개). */
    @Column(nullable = false, unique = true)
    private Long childId;

    /** 실시간 추적 켜짐 여부. */
    @Column(nullable = false)
    private boolean realtimeEnabled;

    public TrackingSetting(Long childId, boolean realtimeEnabled) {
        this.childId = childId;
        this.realtimeEnabled = realtimeEnabled;
    }

    public void setRealtimeEnabled(boolean realtimeEnabled) {
        this.realtimeEnabled = realtimeEnabled;
    }
}
