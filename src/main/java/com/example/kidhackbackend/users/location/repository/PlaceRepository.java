package com.example.kidhackbackend.users.location.repository;

import com.example.kidhackbackend.users.location.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    /** 자녀에 등록된 모든 공간. */
    List<Place> findByChildId(Long childId);

    /** 회원 탈퇴(물리삭제) 시 자녀의 지정 장소 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Place p where p.childId = :childId")
    int deleteByChildId(@Param("childId") Long childId);
}
