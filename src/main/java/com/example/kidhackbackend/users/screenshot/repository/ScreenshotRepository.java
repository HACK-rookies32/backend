package com.example.kidhackbackend.users.screenshot.repository;

import com.example.kidhackbackend.users.screenshot.entity.Screenshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ScreenshotRepository extends JpaRepository<Screenshot, Long> {

    /** 기간 필터가 모두 주어졌을 때. 최신 촬영 순. */
    Page<Screenshot> findByChildIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            Long childId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /** 기간 필터가 없을 때. 최신 촬영 순. */
    Page<Screenshot> findByChildIdOrderByCapturedAtDesc(Long childId, Pageable pageable);

    /** 회원 탈퇴(물리삭제) 시 자녀의 스크린샷 메타데이터 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Screenshot s where s.childId = :childId")
    int deleteByChildId(@Param("childId") Long childId);
}
