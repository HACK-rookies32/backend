package com.example.kidhackbackend.admin.community.repository;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

public interface AdminPostRepository extends JpaRepository<CommunityBoard, Long> {

    @Query("SELECT p, COALESCE(r.reportCount, 0) " +
            "FROM CommunityBoard p " +
            "LEFT JOIN PostReport r ON p.id = r.postId")
    Page<Object[]> findAllWithReportCount(Pageable pageable);

    @Query("SELECT p, r.reportCount " +
            "FROM CommunityBoard p " +
            "JOIN PostReport r ON p.id = r.postId " +
            "WHERE r.reportCount >= 1")
    Page<Object[]> findReportedPosts(Pageable pageable);
}