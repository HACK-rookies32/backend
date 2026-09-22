package com.example.kidhackbackend.users.filtering.repository;

import com.example.kidhackbackend.users.filtering.entity.BlockRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockRuleRepository extends JpaRepository<BlockRule, Long> {

    /** 자녀의 차단 규칙 목록. */
    List<BlockRule> findByChildId(Long childId);

    /** 같은 자녀에 같은 패턴이 이미 등록돼 있는지 — 중복 등록 차단(QA #40). */
    boolean existsByChildIdAndPattern(Long childId, String pattern);

    /** 회원 탈퇴(물리삭제) 시 자녀의 차단 규칙 정리. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BlockRule b where b.childId = :childId")
    int deleteByChildId(@Param("childId") Long childId);
}
