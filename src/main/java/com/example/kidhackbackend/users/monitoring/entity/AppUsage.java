package com.example.kidhackbackend.users.monitoring.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
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
 * 자녀 앱 사용량 기록 — 역할표 No.13(시간대별 모니터링).
 *
 * <p>위치 동선과 통일하여 배치로 적재하고, {@code createdAt} 기준 1주일 롤링 삭제한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "app_usage",
        indexes = {
                @Index(name = "idx_usage_child_recorded", columnList = "childId,recordedAt"),
                @Index(name = "idx_usage_created_at", columnList = "createdAt")
        }
)
public class AppUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long childId;

    @Column(nullable = false)
    private String appName;

    /** 사용 시간(초). */
    @Column(nullable = false)
    private long durationSec;

    /** 사용 기록 시각(집계 기준). */
    @Column(nullable = false)
    private LocalDateTime recordedAt;

    public AppUsage(Long childId, String appName, long durationSec, LocalDateTime recordedAt) {
        this.childId = childId;
        this.appName = appName;
        this.durationSec = durationSec;
        this.recordedAt = recordedAt;
    }
}
