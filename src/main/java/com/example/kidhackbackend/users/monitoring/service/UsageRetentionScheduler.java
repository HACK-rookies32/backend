package com.example.kidhackbackend.users.monitoring.service;

import com.example.kidhackbackend.users.monitoring.repository.AppUsageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 앱 사용량 롤링 삭제 배치 — 역할표 No.13.
 *
 * <p>보관 기간(기본 7일)을 넘긴 기록을 매일 새벽 4시에 벌크 삭제한다. (위치 3일 삭제와 동일 패턴)
 */
@Slf4j
@Component
public class UsageRetentionScheduler {

    private final AppUsageRepository repository;
    private final int retentionDays;

    public UsageRetentionScheduler(AppUsageRepository repository,
                                   @Value("${kidhack.monitoring.retention-days:7}") int retentionDays) {
        this.repository = repository;
        this.retentionDays = retentionDays;
    }

    /** 매일 04:00 실행. */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeExpiredUsages() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        int deleted = repository.deleteByCreatedAtBefore(threshold);
        log.info("[USAGE-RETENTION] {}일 경과 사용량 {}건 삭제 (기준: {})", retentionDays, deleted, threshold);
    }
}
