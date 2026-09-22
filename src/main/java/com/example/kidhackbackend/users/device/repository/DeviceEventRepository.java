package com.example.kidhackbackend.users.device.repository;

import com.example.kidhackbackend.users.device.entity.DeviceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, Long> {

    List<DeviceEvent> findByChildIdOrderByCreatedAtDesc(Long childId);

    /** 회원 탈퇴(물리삭제) 시 자녀의 기기 이벤트 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DeviceEvent d where d.childId = :childId")
    int deleteByChildId(@Param("childId") Long childId);
}
