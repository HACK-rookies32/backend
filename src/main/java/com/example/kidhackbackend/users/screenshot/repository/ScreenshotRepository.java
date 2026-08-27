package com.example.kidhackbackend.users.screenshot.repository;

import com.example.kidhackbackend.users.screenshot.entity.Screenshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ScreenshotRepository extends JpaRepository<Screenshot, Long> {

    /** 기간 필터가 모두 주어졌을 때. 최신 촬영 순. */
    Page<Screenshot> findByChildIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            Long childId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /** 기간 필터가 없을 때. 최신 촬영 순. */
    Page<Screenshot> findByChildIdOrderByCapturedAtDesc(Long childId, Pageable pageable);
}
