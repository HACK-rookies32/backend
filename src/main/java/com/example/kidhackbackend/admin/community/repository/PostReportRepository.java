package com.example.kidhackbackend.admin.community.repository;

import com.example.kidhackbackend.admin.community.entity.PostReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {
    Optional<PostReport> findByPostId(Long postId);
}