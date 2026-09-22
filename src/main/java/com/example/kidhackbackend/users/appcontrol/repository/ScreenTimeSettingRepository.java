package com.example.kidhackbackend.users.appcontrol.repository;

import com.example.kidhackbackend.users.appcontrol.entity.ScreenTimeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScreenTimeSettingRepository extends JpaRepository<ScreenTimeSetting, Long> {

    Optional<ScreenTimeSetting> findByChildId(Long childId);

    /** 회원 탈퇴(물리삭제) 시 자녀의 스크린타임 설정 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ScreenTimeSetting s where s.childId = :childId")
    int deleteByChildId(@Param("childId") Long childId);
}
