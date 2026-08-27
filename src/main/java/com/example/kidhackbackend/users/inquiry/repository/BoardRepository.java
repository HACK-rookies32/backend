package com.example.kidhackbackend.users.inquiry.repository;

import com.example.kidhackbackend.users.inquiry.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {
    Page<Board> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Board> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    // 관리자 - 문의 기록 CSV 리포트용. 작성일(createdAt) 기준 기간 조회.
    List<Board> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}