package com.example.kidhackbackend.global.notification.repository;

import com.example.kidhackbackend.global.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByMemberId(Long memberId);
}