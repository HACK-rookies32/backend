package com.example.kidhackbackend.users.filtering.repository;

import com.example.kidhackbackend.users.filtering.entity.HarmfulKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HarmfulKeywordRepository extends JpaRepository<HarmfulKeyword, Long> {

    boolean existsByKeyword(String keyword);

    /** 최근 등록 순 목록. */
    List<HarmfulKeyword> findAllByOrderByIdDesc();
}
