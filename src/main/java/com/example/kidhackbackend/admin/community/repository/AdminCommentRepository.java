package com.example.kidhackbackend.admin.community.repository;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
public interface AdminCommentRepository extends JpaRepository<CommunityBoardComment, Long> {
    List<CommunityBoardComment> findByBoardId(Long boardId);
    @Query("SELECT c, COALESCE(r.reportCount, 0) " +
            "FROM CommunityBoardComment c " +
            "LEFT JOIN CommentReport r ON c.id = r.commentId " +
            "WHERE c.board.id = :postId")
    List<Object[]> findByPostIdWithReportCount(@Param("postId") Long postId);

    @Query(
            value = "SELECT c, r.reportCount " +
                    "FROM CommunityBoardComment c " +
                    "JOIN CommentReport r ON c.id = r.commentId " +
                    "WHERE r.reportCount >= 1",
            countQuery = "SELECT count(r) FROM CommentReport r WHERE r.reportCount >= 1"
    )
    Page<Object[]> findReportedComments(Pageable pageable);


}