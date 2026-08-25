package com.example.kidhackbackend.users.inquiry.repository;

import com.example.kidhackbackend.users.inquiry.entity.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {
    Optional<InquiryAnswer> findByBoardId(Long boardId);
    void deleteByBoardId(Long boardId);
}