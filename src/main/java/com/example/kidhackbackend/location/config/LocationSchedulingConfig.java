package com.example.kidhackbackend.location.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 위치 도메인 스케줄링 활성화.
 *
 * <p>공통 {@code KidhackBackendApplication}을 수정하지 않고 도메인 내부에서 스케줄링을 켠다.
 * ({@link com.example.kidhackbackend.location.service.TrackRetentionScheduler})
 */
@Configuration
@EnableScheduling
public class LocationSchedulingConfig {
}
