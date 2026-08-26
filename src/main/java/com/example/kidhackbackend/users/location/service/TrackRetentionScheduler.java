package com.example.kidhackbackend.users.location.service;

import com.example.kidhackbackend.users.location.repository.TrackSegmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 동선 세그먼트 롤링 삭제 배치 (역할표 No.2).
 *
 * <p>보관 기간(기본 3일)을 넘긴 세그먼트를 매일 새벽 3시에 벌크 삭제한다.
 */
@Slf4j
@Component
public class TrackRetentionScheduler {

    private final TrackSegmentRepository repository;
    private final int retentionDays;

    public TrackRetentionScheduler(TrackSegmentRepository repository,
                                   @Value("${kidhack.location.retention-days:3}") int retentionDays) {
        this.repository = repository;
        this.retentionDays = retentionDays;
    }

    /** 매일 03:00 실행. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredSegments() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        int deleted = repository.deleteByCreatedAtBefore(threshold);
        log.info("[TRACK-RETENTION] {}일 경과 세그먼트 {}건 삭제 (기준: {})", retentionDays, deleted, threshold);
    }
}
