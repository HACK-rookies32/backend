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

        assertThatCode(() -> remover.remove(board)).doesNotThrowAnyException();

        assertThat(boardRepository.findById(board.getId())).isEmpty();
        assertThat(commentRepository.findByBoardIdOrderByCreatedAtAsc(board.getId())).isEmpty();
    }

    @Test
    void 좋아요가_달린_게시글도_지워진다() {
        CommunityBoard board = post("좋아요 있는 글");
        like(board);

        assertThatCode(() -> remover.remove(board)).doesNotThrowAnyException();

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

        remover.remove(target);

        assertThat(boardRepository.findById(other.getId())).isPresent();
        assertThat(commentRepository.findByBoardIdOrderByCreatedAtAsc(other.getId())).hasSize(1);
        assertThat(likeRepository.countByBoardId(other.getId())).isEqualTo(1);
    }

    /** 첨부는 DB 행뿐 아니라 저장소 실물까지 지워야 한다. */
    @Test
    void 첨부_실물도_지운다() {
        CommunityBoard board = post("첨부 있는 글");
        // 불변 리스트를 넣으면 Hibernate 가 컬렉션을 비울 때 UnsupportedOperationException 이 난다.
        board.setAttachments(new ArrayList<>(
                List.of("/api/files/community/a.png", "/api/files/community/b.png")));
        boardRepository.save(board);

        remover.remove(board);

        Mockito.verify(fileStorageService).delete("/api/files/community/a.png");
        Mockito.verify(fileStorageService).delete("/api/files/community/b.png");
    }
}
