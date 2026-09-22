package com.example.kidhackbackend.users.appcontrol.repository;

import com.example.kidhackbackend.users.appcontrol.entity.AppControlRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppControlRuleRepository extends JpaRepository<AppControlRule, Long> {
    List<AppControlRule> findByChildId(Long childId);

    boolean existsByChildIdAndPackageName(Long childId, String packageName);

    /** 회원 탈퇴(물리삭제) 시 자녀의 앱 제어 규칙 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AppControlRule a where a.childId = :childId")
    int deleteByChildId(@Param("childId") Long childId);
}