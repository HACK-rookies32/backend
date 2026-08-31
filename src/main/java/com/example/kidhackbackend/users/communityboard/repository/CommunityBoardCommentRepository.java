package com.example.kidhackbackend.users.communityboard.repository;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunityBoardCommentRepository extends JpaRepository<CommunityBoardComment, Long> {
    List<CommunityBoardComment> findByBoardIdOrderByCreatedAtAsc(Long boardId);

    Page<CommunityBoardComment> findByMemberId(Long memberId, Pageable pageable);

    /**
     * 게시글의 댓글을 통째로 지운다. 게시글 삭제 전에 부른다.
     *
     * <p>파생 삭제(deleteByBoardId) 대신 벌크 DML 로 두는 이유: 파생 삭제는 엔티티를 읽어
     * 하나씩 remove 하고 실제 SQL 은 flush 시점에 나가므로, 부모 삭제와 순서가 엇갈릴 여지가
     * 있다. 벌크 DML 은 즉시 실행돼 자식이 먼저 사라지는 것이 보장된다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommunityBoardComment c where c.board.id = :boardId")
    int deleteByBoardId(@Param("boardId") Long boardId);

    /**
     * 회원이 <b>남의 글에 단</b> 댓글을 통째로 지운다. 회원 삭제 전에 부른다.
     *
     * <p>본인 글의 댓글은 게시글을 지울 때 {@code deleteByBoardId} 로 함께 사라지지만,
     * 남의 글에 단 댓글은 그 경로로 정리되지 않아 {@code member_id} FK 에 걸린다.
     * 벌크 DML 인 이유는 위와 같다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommunityBoardComment c where c.member.id = :memberId")
    int deleteByMemberId(@Param("memberId") Long memberId);
}