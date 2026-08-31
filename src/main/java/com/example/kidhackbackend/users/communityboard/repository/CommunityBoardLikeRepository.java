package com.example.kidhackbackend.users.communityboard.repository;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityBoardLikeRepository extends JpaRepository<CommunityBoardLike, Long> {
    Optional<CommunityBoardLike> findByBoardIdAndMemberId(Long boardId, Long memberId);
    long countByBoardId(Long boardId);

    Page<CommunityBoardLike> findByMemberId(Long memberId, Pageable pageable);

    /** 게시글의 좋아요를 통째로 지운다. 게시글 삭제 전에 부른다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommunityBoardLike l where l.board.id = :boardId")
    int deleteByBoardId(@Param("boardId") Long boardId);
}