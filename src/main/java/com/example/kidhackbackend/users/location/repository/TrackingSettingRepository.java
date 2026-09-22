package com.example.kidhackbackend.users.location.repository;

import com.example.kidhackbackend.users.location.entity.TrackingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrackingSettingRepository extends JpaRepository<TrackingSetting, Long> {

    Optional<TrackingSetting> findByChildId(Long childId);

    /** 회원 탈퇴(물리삭제) 시 자녀의 위치추적 설정 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TrackingSetting t where t.childId = :childId")
    int deleteByChildId(@Param("childId") Long childId);
}
