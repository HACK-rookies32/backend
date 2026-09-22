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

    /** 목록 미리보기 길이. 320dp 폭에서 한 줄이 대략 이만큼이다. */
    private static final int PREVIEW_LENGTH = 60;

    private final CommunityBoardRepository communityBoardRepository;
    private final CommunityBoardCommentRepository commentRepository;
    private final CommunityBoardLikeRepository likeRepository;
    private final FileStorageService fileStorageService;
    private final CommunityPostRemover postRemover;
    private final MemberBanRepository memberBanRepository;
    private final AttachmentValidator attachmentValidator;
    private final CommunityThumbnailService thumbnailService;

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
        // [VULN-11] 위와 동일 — 작성자 검증이 없어 postId 만 알면 남의 글을 지울 수 있다.
        //
        //   삭제는 DB 행에서 끝나지 않는다. postRemover 가 좋아요·댓글·글을 지운 뒤
        //   첨부 파일의 S3 실물까지 지우므로, 피해가 저장소로 번진다.
        //
        //   복구 가능성(2026-09-07 인프라 설정 확인 결과, 낙관하면 안 된다):
        //     · S3 버킷은 버저닝이 켜져 있고 코드는 DeleteObject 만 호출하므로 이전 버전은 남지만,
        //       자동 복구는 없고 운영자가 손으로 되살려야 한다.
        //     · 노후 버전 만료 정책이 30일이라 그 안에 알아채지 못하면 영구히 사라진다.
        //     · DB 백업 보존은 7일이라 기간이 달라, 글과 첨부를 같은 시점으로 함께 되돌릴 수 없다.
        //   방어: if (!board.getMember().getId().equals(member.getId())) throw new AccessDenied...;
        // 댓글·좋아요를 먼저 걷어내지 않으면 FK 에 걸려 500 이 난다. 관리자 삭제와 같은 경로를 쓴다.
        postRemover.remove(board);
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
                preview(board.getContent()),
                // 첫 첨부의 공개 경로를 썸네일 프록시 주소로 감싼다([VULN-SSRF] 싱크로 연결).
                board.getAttachments().isEmpty()
                        ? null
                        : thumbnailService.toThumbnailUrl(board.getAttachments().get(0)),
                board.getMember().getNickname(),
                likeRepository.countByBoardId(board.getId()),
                board.getCreatedAt()
        );
    }

    /** 목록 한 줄에 들어갈 만큼만 자른다. 줄바꿈은 공백으로 눕혀야 한 줄로 읽힌다. */
    private static String preview(String content) {
        if (content == null) {
            return null;
        }
        String flat = content.replaceAll("\\s+", " ").trim();
        return flat.length() <= PREVIEW_LENGTH ? flat : flat.substring(0, PREVIEW_LENGTH) + "…";
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
