package com.example.kidhackbackend.users.location.repository;

import com.example.kidhackbackend.users.location.entity.TrackSegment;
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

    /** 자녀의 최근 동선 조회(자녀 관리 페이지 연동, No.18). 최신 세그먼트가 먼저. */
    List<TrackSegment> findByChildIdOrderBySegmentStartDesc(Long childId);

    /** 동선 그리기용 — 시간 오름차순 전체 조회. */
    List<TrackSegment> findByChildIdOrderBySegmentStartAsc(Long childId);

    /** 동선 그리기용 — [from, to) 구간을 시간 오름차순으로 조회. */
    @Query("select s from TrackSegment s where s.childId = :childId "
            + "and s.segmentStart >= :from and s.segmentStart < :to order by s.segmentStart asc")
    List<TrackSegment> findTrackInRange(@Param("childId") Long childId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /** 3일 롤링 삭제(No.2): 기준 시각보다 오래된 세그먼트 벌크 삭제. */
    @Modifying
    @Query("delete from TrackSegment s where s.createdAt < :threshold")
    int deleteByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);

    /** 회원 탈퇴(물리삭제) 시 자녀의 동선(세그먼트) 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TrackSegment s where s.childId = :childId")
    int deleteByChildId(@Param("childId") Long childId);
}
