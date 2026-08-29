package com.example.kidhackbackend.users.inquiry.repository;

import com.example.kidhackbackend.users.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Inquiry> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    // 관리자 - 문의 기록 CSV 리포트용. 작성일(createdAt) 기준 기간 조회.
    List<Inquiry> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}
