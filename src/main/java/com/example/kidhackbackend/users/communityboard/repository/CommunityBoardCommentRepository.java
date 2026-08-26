package com.example.kidhackbackend.users.communityboard.repository;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityBoardCommentRepository extends JpaRepository<CommunityBoardComment, Long> {
    List<CommunityBoardComment> findByBoardIdOrderByCreatedAtAsc(Long boardId);

    Page<CommunityBoardComment> findByMemberId(Long memberId, Pageable pageable);
}