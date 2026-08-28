package com.example.kidhackbackend.users.appcontrol.repository;

import com.example.kidhackbackend.users.appcontrol.entity.AppControlRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppControlRuleRepository extends JpaRepository<AppControlRule, Long> {
    List<AppControlRule> findByChildId(Long childId);

    boolean existsByChildIdAndPackageName(Long childId, String packageName);
}