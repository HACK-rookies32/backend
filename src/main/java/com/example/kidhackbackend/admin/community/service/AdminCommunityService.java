package com.example.kidhackbackend.admin.community.service;

import com.example.kidhackbackend.admin.community.dto.AdminCommentResponse;
import com.example.kidhackbackend.admin.community.dto.AdminPostDetailResponse;
import com.example.kidhackbackend.admin.community.dto.AdminPostResponse;
import com.example.kidhackbackend.admin.community.entity.CommentReport;
import com.example.kidhackbackend.admin.community.entity.PostReport;
import com.example.kidhackbackend.admin.community.repository.AdminCommentRepository;
import com.example.kidhackbackend.admin.community.repository.AdminPostRepository;
import com.example.kidhackbackend.admin.community.repository.CommentReportRepository;
import com.example.kidhackbackend.admin.community.repository.PostReportRepository;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardComment;
import com.example.kidhackbackend.users.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCommunityService {

    private final AdminPostRepository adminPostRepository;
    private final AdminCommentRepository adminCommentRepository;
    private final PostReportRepository postReportRepository;
    private final CommentReportRepository commentReportRepository;

    // 사용자 신고 접수 (게시글)
    @Transactional
    public void reportPost(Long postId, Member member) {
        CommunityBoard post = adminPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (post.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인이 작성한 게시글은 신고할 수 없습니다.");
        }

        postReportRepository.findByPostId(postId)
                .ifPresentOrElse(
                        PostReport::increaseReportCount,
                        () -> postReportRepository.save(new PostReport(postId))
                );
    }

    // 사용자 신고 접수 (댓글)
    @Transactional
    public void reportComment(Long postId, Long commentId, Member member) {
        CommunityBoardComment comment = adminCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        if (!comment.getBoard().getId().equals(postId)) {
            throw new IllegalArgumentException("해당 게시글에 등록된 댓글이 아닙니다.");
        }

        if (comment.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인이 작성한 댓글은 신고할 수 없습니다.");
        }

        commentReportRepository.findByCommentId(commentId)
                .ifPresentOrElse(
                        CommentReport::increaseReportCount,
                        () -> commentReportRepository.save(new CommentReport(commentId))
                );
    }

    // 전체 게시글 페이징 조회
    public Page<AdminPostResponse> getAllPosts(Pageable pageable) {
        return adminPostRepository.findAllWithReportCount(pageable)
                .map(result -> new AdminPostResponse((CommunityBoard) result[0], (int) result[1]));
    }

    // 게시글 상세 조회
    public AdminPostDetailResponse getPostDetail(Long id) {
        CommunityBoard post = adminPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        int reportCount = postReportRepository.findByPostId(id)
                .map(PostReport::getReportCount)
                .orElse(0);

        List<AdminCommentResponse> comments = adminCommentRepository.findByPostIdWithReportCount(id).stream()
                .map(result -> new AdminCommentResponse((CommunityBoardComment) result[0], (int) result[1]))
                .collect(Collectors.toList());

        return new AdminPostDetailResponse(post, reportCount, comments);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long id) {
        if (!adminPostRepository.existsById(id)) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
        }

        // 1. 해당 게시글의 댓글들에 연결된 CommentReport 정리
        List<CommunityBoardComment> comments = adminCommentRepository.findByBoardId(id);
        for (CommunityBoardComment comment : comments) {
            commentReportRepository.findByCommentId(comment.getId())
                    .ifPresent(commentReportRepository::delete);
        }

        // 2. 게시글 신고 데이터(PostReport) 정리
        postReportRepository.findByPostId(id)
                .ifPresent(postReportRepository::delete);

        // 3. 게시글 삭제
        adminPostRepository.deleteById(id);
    }

    // 신고된(report >= 1) 게시글 목록 페이징 조회
    public Page<AdminPostResponse> getReportedPosts(Pageable pageable) {
        return adminPostRepository.findReportedPosts(pageable)
                .map(result -> new AdminPostResponse((CommunityBoard) result[0], (int) result[1]));
    }

    // 신고된(report >= 1) 댓글 목록 페이징 조회
    public Page<AdminCommentResponse> getReportedComments(Pageable pageable) {
        return adminCommentRepository.findReportedComments(pageable)
                .map(result -> new AdminCommentResponse((CommunityBoardComment) result[0], (int) result[1]));
    }

    // 신고된 댓글 단건 상세 조회
    public AdminCommentResponse getReportedCommentDetail(Long id) {
        CommunityBoardComment comment = adminCommentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));
        int reportCount = commentReportRepository.findByCommentId(id)
                .map(CommentReport::getReportCount)
                .orElse(0);
        return new AdminCommentResponse(comment, reportCount);
    }


}