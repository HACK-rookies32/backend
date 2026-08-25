package com.example.kidhackbackend.admin.community.repository;

import com.example.kidhackbackend.admin.community.entity.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    Optional<CommentReport> findByCommentId(Long commentId);
}