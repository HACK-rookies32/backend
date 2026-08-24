package com.example.kidhackbackend.location.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 임시 알림 어댑터 — 알림 도메인 완성 전까지 로그로만 기록한다.
 */
@Slf4j
@Component
public class LogAlertAdapter implements AlertPort {

    @Override
    public void sendLocationAlert(Long childId, String type, String message) {
        log.info("[LOCATION-ALERT] childId={} type={} message={}", childId, type, message);
    }
}
