package com.example.kidhackbackend.users.communityboard.repository;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityBoardRepository extends JpaRepository<CommunityBoard, Long> {

    Page<CommunityBoard> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String title, String content, Pageable pageable);
}