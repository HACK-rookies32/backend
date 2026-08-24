package com.example.kidhackbackend.location.repository;

import com.example.kidhackbackend.location.entity.TrackSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TrackSegmentRepository extends JpaRepository<TrackSegment, Long> {

    /** 특정 자녀의 특정 5분 창(세그먼트) 조회 — 창 갱신/신규 판정에 사용. */
    Optional<TrackSegment> findByChildIdAndSegmentStart(Long childId, LocalDateTime segmentStart);

    /** 자녀의 최근 동선 조회(자녀 관리 페이지 연동, No.18). */
    List<TrackSegment> findByChildIdOrderBySegmentStartDesc(Long childId);

    /** 3일 롤링 삭제(No.2): 기준 시각보다 오래된 세그먼트 벌크 삭제. */
    @Modifying
    @Query("delete from TrackSegment s where s.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}
