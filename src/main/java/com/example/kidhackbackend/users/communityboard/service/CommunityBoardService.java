package com.example.kidhackbackend.users.communityboard.service;

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

    @Transactional
    public PostResponse createPost(Member member, PostRequest request) {
        CommunityBoard board = new CommunityBoard();
        board.setMember(member);
        board.setTitle(request.getTitle());
        board.setContent(request.getContent());
        if (request.getAttachments() != null) board.setAttachments(request.getAttachments());
        if (request.getLinks() != null) board.setLinks(request.getLinks());

        communityBoardRepository.save(board);
        return toResponse(board);
    }

    public PostPageResponse getPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String kw = (keyword == null) ? "" : keyword;
        Page<CommunityBoard> result = communityBoardRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(kw, kw, pageable);

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
        CommunityBoard board = findBoard(postId);
        return toDetailResponse(board);
    }

    @Transactional
    public PostResponse updatePost(Long postId, Member member, PostRequest request) {
        CommunityBoard board = findBoard(postId);
        // [VULN-11] board.getMember()와 파라미터로 받은 member를 비교하는 로직이 없음(IDOR)
        //           로그인은 확인하지만 실제 작성자인지는 검증하지 않아, id만 알면 남의 글을 수정 가능
        if (request.getTitle() != null) board.setTitle(request.getTitle());
        if (request.getContent() != null) board.setContent(request.getContent());
        if (request.getAttachments() != null) board.setAttachments(request.getAttachments());
        if (request.getLinks() != null) board.setLinks(request.getLinks());
        return toResponse(board);
    }

    @Transactional
    public void deletePost(Long postId, Member member) {
        CommunityBoard board = findBoard(postId);
        // [VULN-11] 위와 동일한 이유로, 작성자 검증 없이 삭제까지 가능함
        //
        // ⚠️ 아래 첨부 삭제가 이 취약점의 영향 범위를 DB 행에서 실제 파일로 넓힌다.
        //    다만 운영 버킷은 버저닝이 켜져 있어 삭제 마커만 남고, 라이프사이클이
        //    이전 버전을 만료시키기 전까지는 복구할 수 있다(infra storage.tf).
        //    첨부를 남겨 두면 고아 객체가 무한히 쌓이므로 지우는 쪽을 택했다.
        board.getAttachments().forEach(fileStorageService::delete);
        communityBoardRepository.delete(board);
    }

    @Transactional
    public CommentResponse createComment(Long postId, Member member, CommentRequest request) {
        CommunityBoard board = findBoard(postId);
        CommunityBoardComment comment = new CommunityBoardComment();
        comment.setBoard(board);
        comment.setMember(member);
        comment.setContent(request.getContent());
        commentRepository.save(comment);

        return new CommentResponse(
                comment.getId(),
                member.getId(),
                member.getNickname(),
                comment.getContent(),
                comment.getCreatedAt()
        );
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

    private CommunityBoard findBoard(Long postId) {
        return communityBoardRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    }

    private PostResponse toResponse(CommunityBoard board) {
        long likeCount = likeRepository.countByBoardId(board.getId());
        return new PostResponse(
                board.getId(),
                board.getTitle(),
                board.getMember().getNickname(),
                likeCount,
                board.getCreatedAt()
        );
    }

    private PostDetailResponse toDetailResponse(CommunityBoard board) {
        long likeCount = likeRepository.countByBoardId(board.getId());
        return new PostDetailResponse(
                board.getId(),
                board.getMember().getId(),
                board.getMember().getNickname(),
                board.getTitle(),
                board.getContent(),
                board.getAttachments(),
                board.getLinks(),
                likeCount,
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}