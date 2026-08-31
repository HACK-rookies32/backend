package com.example.kidhackbackend.users.communityboard.service;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardCommentRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardLikeRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 하나를 자식까지 정리하며 지운다.
 *
 * <p>{@code CommunityBoard} 에는 자식 컬렉션(@OneToMany)이 없고 댓글·좋아요 쪽만 부모를
 * 가리킨다. 그래서 게시글 행만 지우려 하면 자식의 {@code post_id} FK 에 걸려 500 이 났다.
 *
 * <p>삭제 경로가 <b>회원용·관리자용 둘</b>이라 각자 정리하게 두면 한쪽만 고쳐져 다시
 * 어긋난다. 순서(좋아요 → 댓글 → 첨부 실물 → 게시글)를 여기 한 곳에 모아 둔다.
 *
 * <p>신고(PostReport·CommentReport)는 FK 가 아니라 {@code Long postId} 컬럼이라 삭제를
 * 막지 않는다. 그쪽 정리는 관리자 서비스가 이미 하고 있어 여기서 건드리지 않는다.
 */
@Component
@RequiredArgsConstructor
public class CommunityPostRemover {

    private final CommunityBoardRepository boardRepository;
    private final CommunityBoardCommentRepository commentRepository;
    private final CommunityBoardLikeRepository likeRepository;
    private final FileStorageService fileStorageService;

    /**
     * 게시글과 딸린 것들을 지운다.
     *
     * <p>첨부는 DB 행뿐 아니라 <b>저장소의 실물</b>까지 지운다. 관리자 삭제도 이 경로를
     * 타므로 회원 삭제와 동작이 같아진다(예전에는 관리자 삭제만 파일을 남겼다).
     */
    @Transactional
    public void remove(CommunityBoard board) {
        Long postId = board.getId();
        likeRepository.deleteByBoardId(postId);
        commentRepository.deleteByBoardId(postId);
        board.getAttachments().forEach(fileStorageService::delete);
        boardRepository.delete(board);
    }
}
