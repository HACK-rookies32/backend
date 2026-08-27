package com.example.kidhackbackend.global.notification.repository;

import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /** 계정당 토큰 하나(memberId 에 unique). */
    Optional<DeviceToken> findByMemberId(Long memberId);

    /** 공지 전체발송처럼 여러 계정의 토큰을 한 번에 모을 때. */
    List<DeviceToken> findAllByMemberIdIn(Collection<Long> memberIds);

    /** FCM 이 무효라고 답한 토큰 정리. */
    void deleteByTokenIn(Collection<String> tokens);
}