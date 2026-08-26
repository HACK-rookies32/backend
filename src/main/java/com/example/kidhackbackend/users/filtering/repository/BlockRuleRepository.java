package com.example.kidhackbackend.users.filtering.repository;

import com.example.kidhackbackend.users.filtering.entity.BlockRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockRuleRepository extends JpaRepository<BlockRule, Long> {

    /** 자녀의 차단 규칙 목록. */
    List<BlockRule> findByChildId(Long childId);
}
