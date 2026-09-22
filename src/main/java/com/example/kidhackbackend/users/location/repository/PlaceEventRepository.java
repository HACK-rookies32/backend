package com.example.kidhackbackend.users.location.repository;

import com.example.kidhackbackend.users.location.entity.PlaceEvent;
import com.example.kidhackbackend.users.location.entity.PlaceEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlaceEventRepository extends JpaRepository<PlaceEvent, Long> {

    /** 특정 공간에 대한 가장 최근 이벤트 — 진입/이탈 상태 전환 판정에 사용. */
    Optional<PlaceEvent> findFirstByChildIdAndPlaceIdOrderByOccurredAtDesc(Long childId, Long placeId);

    /** 도착(ARRIVED) 이력 존재 여부 — 도착 1회 발송(중복 방지)에 사용. */
    boolean existsByChildIdAndPlaceIdAndEventType(Long childId, Long placeId, PlaceEventType eventType);

    /** 자녀의 이벤트 이력(최신순). */
    List<PlaceEvent> findByChildIdOrderByOccurredAtDesc(Long childId);

    /** 자녀의 이벤트 이력 — [from, to) 구간을 최신순으로 조회. */
    @Query("select e from PlaceEvent e where e.childId = :childId "
            + "and e.occurredAt >= :from and e.occurredAt < :to order by e.occurredAt desc")
    List<PlaceEvent> findEventsInRange(@Param("childId") Long childId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /** 회원 탈퇴(물리삭제) 시 자녀의 진입/이탈 이벤트 이력 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PlaceEvent e where e.childId = :childId")
    int deleteByChildId(@Param("childId") Long childId);
}
