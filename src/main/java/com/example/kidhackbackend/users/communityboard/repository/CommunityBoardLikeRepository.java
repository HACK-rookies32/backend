package com.example.kidhackbackend.users.communityboard.repository;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityBoardLikeRepository extends JpaRepository<CommunityBoardLike, Long> {
    Optional<CommunityBoardLike> findByBoardIdAndMemberId(Long boardId, Long memberId);
    long countByBoardId(Long boardId);
}