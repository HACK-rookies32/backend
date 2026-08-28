package com.example.kidhackbackend.users.pairing.repository;

import com.example.kidhackbackend.users.pairing.entity.PairingCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PairingCodeRepository extends JpaRepository<PairingCode, Long> {

    boolean existsByCode(String code);

    /** 부모가 발급한 코드 중 아직 만료되지 않은 것(최신순). */
    List<PairingCode> findByParentIdAndExpiresAtAfterOrderByExpiresAtDesc(Long parentId, LocalDateTime now);

    /** 만료된 코드 정리(스케줄러 전용). */
    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
