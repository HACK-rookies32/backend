package com.example.kidhackbackend.global.notification.repository;

import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /** 등록 시 기기 식별 키. 같은 토큰은 한 행뿐이다. */
    Optional<DeviceToken> findByToken(String token);

    /** 한 계정의 모든 기기 토큰(기기 여러 대 가능). */
    List<DeviceToken> findByMemberId(Long memberId);

    /** 전체발송처럼 여러 계정의 토큰을 한 번에 모을 때. */
    List<DeviceToken> findAllByMemberIdIn(Collection<Long> memberIds);

    /** FCM 이 무효라고 답한 토큰 정리. 호출부가 트랜잭션 밖일 수 있어 직접 건다. */
    @Transactional
    void deleteByTokenIn(Collection<String> tokens);

    /** 로그아웃 시 그 기기 토큰만 해제. memberId 로 스코프해 남의 토큰은 못 지운다. */
    @Transactional
    void deleteByTokenAndMemberId(String token, Long memberId);
}
