package com.example.kidhackbackend.users.communityboard.service;

import com.example.kidhackbackend.admin.community.entity.MemberBan;
import com.example.kidhackbackend.admin.community.repository.MemberBanRepository;
import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.storage.AttachmentValidator;
import com.example.kidhackbackend.global.storage.UploadCategory;
import com.example.kidhackbackend.users.communityboard.dto.*;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardComment;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardLike;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardCommentRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardLikeRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityBoardService {

    private final CommunityBoardRepository communityBoardRepository;
    private final CommunityBoardCommentRepository commentRepository;
    private final CommunityBoardLikeRepository likeRepository;
    private final FileStorageService fileStorageService;
    private final MemberBanRepository memberBanRepository;
    private final AttachmentValidator attachmentValidator;

    @Transactional
    public PostResponse createPost(Member member, PostRequest request) {
        checkBan(member);
        attachmentValidator.requireAllUploaded(request.getAttachments(), UploadCategory.COMMUNITY);
        CommunityBoard board = new CommunityBoard();
        board.setMember(member);
        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        if (request.getAttachments() != null) board.setAttachments(request.getAttachments());
        if (request.getLinks() != null) board.setLinks(request.getLinks());

        communityBoardRepository.save(board);
        return toResponse(board);
    }

    /** 게시글 목록/검색 — 최신순. keyword 는 제목·작성자 닉네임에서 매칭. */
    public PostPageResponse getPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String kw = (keyword == null) ? "" : keyword.trim();
        Page<CommunityBoard> result = communityBoardRepository.searchLatest(kw, pageable);

        List<PostResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PostPageResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PostDetailResponse getPostDetail(Long postId) {
        return toDetailResponse(findBoard(postId));
    }

    @Transactional
    public PostResponse updatePost(Long postId, Member member, PostRequest request) {
        CommunityBoard board = findBoard(postId);
        attachmentValidator.requireAllUploaded(request.getAttachments(), UploadCategory.COMMUNITY);
        // [VULN-11] board.getMember() 와 요청자(member) 비교가 없음(IDOR) — 로그인만 확인하고
        //           실제 작성자인지는 검증하지 않아, postId 만 알면 남의 글을 수정할 수 있다.
        if (request.getTitle() != null) board.setTitle(request.getTitle());
        if (request.getContent() != null) board.setContent(request.getContent());
        if (request.getAttachments() != null) board.setAttachments(request.getAttachments());
        if (request.getLinks() != null) board.setLinks(request.getLinks());
        return toResponse(board);
    }

    @Transactional
    public void deletePost(Long postId, Member member) {
        CommunityBoard board = findBoard(postId);
        // [VULN-11] 위와 동일 — 작성자 검증 없이 삭제까지 가능. 아래 첨부 삭제가 피해 범위를
        //           DB 행에서 실제 S3 객체로 넓힌다(운영 버킷은 버저닝이라 복구 여지는 있음).
        board.getAttachments().forEach(fileStorageService::delete);
        communityBoardRepository.delete(board);
    }

    @Transactional
    public CommentResponse createComment(Long postId, Member member, CommentRequest request) {
        checkBan(member);
        CommunityBoard board = findBoard(postId);
        CommunityBoardComment comment = new CommunityBoardComment();
        comment.setBoard(board);
        comment.setMember(member);
        comment.setContent(request.getContent());
        commentRepository.save(comment);
        return toCommentResponse(comment);
    }

    /** 댓글 삭제 — 본인 댓글만(작성자 검증). 이 게시글의 댓글이 아니거나 없으면 404, 남의 댓글이면 403. */
    @Transactional
    public void deleteComment(Long postId, Long commentId, Member member) {
        CommunityBoardComment comment = commentRepository.findById(commentId)
                .filter(c -> c.getBoard().getId().equals(postId))
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 댓글입니다."));
        if (!comment.getMember().getId().equals(member.getId())) {
            throw new AdminAccessDeniedException("본인 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.delete(comment);
    }

    @Transactional
    public LikeResponse toggleLike(Long postId, Member member) {
        CommunityBoard board = findBoard(postId);

        var existing = likeRepository.findByBoardIdAndMemberId(postId, member.getId());
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            liked = false;
        } else {
            CommunityBoardLike like = new CommunityBoardLike();
            like.setBoard(board);
            like.setMember(member);
            likeRepository.save(like);
            liked = true;
        }

        long count = likeRepository.countByBoardId(postId);
        return new LikeResponse(liked, count);
    }

    /* ---------------------------------------------------------------- 내부 ---- */

    private CommunityBoard findBoard(Long postId) {
        return communityBoardRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다."));
    }

    private void checkBan(Member member) {
        memberBanRepository.findByMemberId(member.getId())
                .filter(MemberBan::isBanned)
                .ifPresent(ban -> {
                    String until = ban.getBannedUntil()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm"));
                    throw new IllegalArgumentException(
                            "커뮤니티 이용이 정지된 상태입니다. (" + until + "까지)");
                });
    }

    private PostResponse toResponse(CommunityBoard board) {
        return new PostResponse(
                board.getId(),
                board.getMember().getId(),
                board.getTitle(),
                board.getMember().getNickname(),
                likeRepository.countByBoardId(board.getId()),
                board.getCreatedAt()
        );
    }

    private PostDetailResponse toDetailResponse(CommunityBoard board) {
        List<CommentResponse> comments = commentRepository
                .findByBoardIdOrderByCreatedAtAsc(board.getId()).stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());

        return new PostDetailResponse(
                board.getId(),
                board.getMember().getId(),
                board.getMember().getNickname(),
                board.getTitle(),
                board.getContent(),
                board.getAttachments(),
                board.getLinks(),
                likeRepository.countByBoardId(board.getId()),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                comments
        );
    }

    private CommentResponse toCommentResponse(CommunityBoardComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getMember().getId(),
                comment.getMember().getNickname(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
