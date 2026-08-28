package com.example.kidhackbackend.users.inquiry.repository;

import com.example.kidhackbackend.users.inquiry.entity.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {
    Optional<InquiryAnswer> findByInquiryId(Long inquiryId);
    void deleteByInquiryId(Long inquiryId);

    // 관리자 - 문의 기록 CSV 리포트용. 문의별 답변을 건건이 조회하지 않도록 한 번에 가져온다.
    List<InquiryAnswer> findByInquiryIdIn(Collection<Long> inquiryIds);
}
