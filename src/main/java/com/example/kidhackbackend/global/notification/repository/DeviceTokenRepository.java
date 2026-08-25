package com.example.kidhackbackend.global.notification.repository;

import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByMemberId(Long memberId);
}