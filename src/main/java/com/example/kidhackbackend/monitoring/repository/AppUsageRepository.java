package com.example.kidhackbackend.monitoring.repository;

import com.example.kidhackbackend.monitoring.entity.AppUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppUsageRepository extends JpaRepository<AppUsage, Long> {

    /** 집계 대상: 자녀의 기간 내 사용 기록. */
    List<AppUsage> findByChildIdAndRecordedAtBetween(Long childId, LocalDateTime from, LocalDateTime to);

    /** 1주일 롤링 삭제(No.13): 기준 시각보다 오래된 기록 벌크 삭제. */
    @Modifying
    @Query("delete from AppUsage u where u.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}
