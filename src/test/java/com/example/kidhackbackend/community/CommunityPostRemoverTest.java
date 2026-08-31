package com.example.kidhackbackend.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.users.communityboard.service.FileStorageService;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardComment;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardLike;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardCommentRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardLikeRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardRepository;
import com.example.kidhackbackend.users.communityboard.service.CommunityPostRemover;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 댓글·좋아요가 달린 게시글이 지워지는지.
 *
 * <p>{@code CommunityBoard} 에 자식 컬렉션이 없어 부모만 지우면 자식의 {@code post_id} FK 에
 * 걸려 500 이 났다. 관리자 웹 삭제 버튼과 모바일 「내 글 삭제」가 둘 다 막혀 있던 결함이라,
 * 실제 스키마로 지워지는지를 봐야 해서 슬라이스 테스트로 둔다(모의 객체로는 FK 를 못 잡는다).
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class CommunityPostRemoverTest {

    @Autowired CommunityBoardRepository boardRepository;
    @Autowired CommunityBoardCommentRepository commentRepository;
    @Autowired CommunityBoardLikeRepository likeRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager entityManager;

    private CommunityPostRemover remover;
    private FileStorageService fileStorageService;
    private Member author;

    @BeforeEach
    void setUp() {
        // 저장소는 파일 실물을 지우는 협력자라 슬라이스에 없다. 호출 여부만 보면 된다.
        fileStorageService = Mockito.mock(FileStorageService.class);
        remover = new CommunityPostRemover(
                boardRepository, commentRepository, likeRepository, fileStorageService);
        author = memberRepository.save(member("writer@test.com"));
    }

    private Member member(String email) {
        Member m = new Member();
        m.setLoginId(email);
        m.setPassword("pw");
        m.setUsername("작성자");
        m.setEmail(email);
        m.setRole(Role.PARENT);
        // birth_date·phone_number 가 NOT NULL 이다.
        m.setBirthDate(LocalDate.of(1990, 1, 1));
        m.setPhoneNumber("010-0000-0000");
        return m;
    }

    private CommunityBoard post(String title) {
        CommunityBoard board = new CommunityBoard();
        board.setMember(author);
        board.setTitle(title);
        board.setContent("본문");
        return boardRepository.save(board);
    }

    /**
     * 컨트롤러가 실제로 넘기는 것과 같은 상태로 다시 읽는다.
     *
     * <p>테스트 안에서 방금 만든 엔티티는 컬렉션이 이미 초기화돼 있어, 지연 로딩이
     * 끊기는 문제를 드러내지 못한다. 요청마다 DB 에서 새로 읽는 실제 흐름과 맞추려면
     * 영속성 컨텍스트를 비우고 다시 조회해야 한다.
     */
    private CommunityBoard reload(CommunityBoard board) {
        entityManager.flush();
        entityManager.clear();
        return boardRepository.findById(board.getId()).orElseThrow();
    }

    private void comment(CommunityBoard board) {
        CommunityBoardComment c = new CommunityBoardComment();
        c.setBoard(board);
        c.setMember(author);
        c.setContent("댓글");
        commentRepository.save(c);
    }

    private void like(CommunityBoard board) {
        CommunityBoardLike l = new CommunityBoardLike();
        l.setBoard(board);
        l.setMember(author);
        likeRepository.save(l);
    }

    @Test
    void 댓글이_달린_게시글도_지워진다() {
        CommunityBoard board = post("댓글 있는 글");
        comment(board);
        comment(board);

        CommunityBoard loaded = reload(board);
        assertThatCode(() -> remover.remove(loaded)).doesNotThrowAnyException();

        assertThat(boardRepository.findById(board.getId())).isEmpty();
        assertThat(commentRepository.findByBoardIdOrderByCreatedAtAsc(board.getId())).isEmpty();
    }

    @Test
    void 좋아요가_달린_게시글도_지워진다() {
        CommunityBoard board = post("좋아요 있는 글");
        like(board);

        CommunityBoard loaded = reload(board);
        assertThatCode(() -> remover.remove(loaded)).doesNotThrowAnyException();

        assertThat(boardRepository.findById(board.getId())).isEmpty();
        assertThat(likeRepository.countByBoardId(board.getId())).isZero();
    }

    /** 다른 글의 댓글·좋아요까지 쓸어 가면 안 된다. */
    @Test
    void 다른_게시글의_댓글은_남는다() {
        CommunityBoard target = post("지울 글");
        comment(target);
        CommunityBoard other = post("남길 글");
        comment(other);
        like(other);

        remover.remove(reload(target));

        assertThat(boardRepository.findById(other.getId())).isPresent();
        assertThat(commentRepository.findByBoardIdOrderByCreatedAtAsc(other.getId())).hasSize(1);
        assertThat(likeRepository.countByBoardId(other.getId())).isEqualTo(1);
    }

    /**
     * 첨부는 DB 행뿐 아니라 저장소 실물까지 지워야 한다.
     *
     * <p>{@code attachments} 는 지연 로딩이라, 자식 벌크 삭제가 영속성 컨텍스트를 비운
     * 뒤에 읽으면 LazyInitializationException 이 난다. 실제로 실서버에서 이 순서 때문에
     * 500 이 났고, 컬렉션이 이미 초기화된 엔티티로 테스트해 놓쳤다 — 그래서 여기서도
     * 반드시 다시 읽은 엔티티로 검증한다.
     */
    @Test
    void 다시_읽은_글도_첨부까지_지운다() {
        CommunityBoard board = post("첨부 있는 글");
        // 불변 리스트를 넣으면 Hibernate 가 컬렉션을 비울 때 UnsupportedOperationException 이 난다.
        board.setAttachments(new ArrayList<>(
                List.of("/api/files/community/a.png", "/api/files/community/b.png")));
        boardRepository.save(board);
        comment(board);

        CommunityBoard loaded = reload(board);
        assertThatCode(() -> remover.remove(loaded)).doesNotThrowAnyException();

        Mockito.verify(fileStorageService).delete("/api/files/community/a.png");
        Mockito.verify(fileStorageService).delete("/api/files/community/b.png");
    }
}
