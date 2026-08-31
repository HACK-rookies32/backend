package com.example.kidhackbackend.users.communityboard.service;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardCommentRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardLikeRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
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
     *
     * <p>순서가 중요하다.
     * <ol>
     *   <li>첨부 경로를 <b>먼저</b> 읽어 복사한다. 아래 벌크 삭제가 영속성 컨텍스트를
     *       비우면서 board 가 준영속이 되는데, {@code attachments} 는 지연 로딩이라
     *       그 뒤에 읽으면 {@code LazyInitializationException} 이 난다.</li>
     *   <li>자식(좋아요·댓글)을 지운다. 먼저 지우지 않으면 FK 에 걸린다.</li>
     *   <li>게시글은 id 로 지운다. 준영속 엔티티를 그대로 넘기면 재부착이 필요하다.</li>
     *   <li>DB 가 지워진 뒤에 파일 실물을 지운다. 트랜잭션이 뒤집혀도 파일만 사라지는
     *       상황을 줄인다.</li>
     * </ol>
     */
    @Transactional
    public void remove(CommunityBoard board) {
        Long postId = board.getId();
        List<String> attachments = List.copyOf(board.getAttachments());

        likeRepository.deleteByBoardId(postId);
        commentRepository.deleteByBoardId(postId);
        boardRepository.deleteById(postId);

        attachments.forEach(fileStorageService::delete);
    }
}
