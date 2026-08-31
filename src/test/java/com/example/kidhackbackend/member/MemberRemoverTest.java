package com.example.kidhackbackend.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardComment;
import com.example.kidhackbackend.users.communityboard.entity.CommunityBoardLike;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardCommentRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardLikeRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardRepository;
import com.example.kidhackbackend.users.communityboard.service.CommunityPostRemover;
import com.example.kidhackbackend.users.communityboard.service.FileStorageService;
import com.example.kidhackbackend.users.inquiry.entity.Inquiry;
import com.example.kidhackbackend.users.inquiry.repository.InquiryRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.member.service.MemberRemover;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 자식이 매달린 회원이 지워지는지.
 *
 * <p>{@code Member} 를 필수 FK 로 참조하는 자식이 6종인데 cascade 가 없어, 회원 행만 지우면
 * FK 위반이 났다. 탈퇴 정리 배치가 1분마다 그 예외로 터지고 <b>한 명이 걸리면 배치 전체가
 * 롤백</b>돼 이후 아무도 정리되지 않았다.
 *
 * <p>모의 객체로는 FK 를 못 잡으므로 실제 스키마를 쓰는 슬라이스 테스트로 둔다
 * ({@code CommunityPostRemoverTest} 와 같은 이유).
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class MemberRemoverTest {

    @Autowired MemberRepository memberRepository;
    @Autowired CommunityBoardRepository boardRepository;
    @Autowired CommunityBoardCommentRepository commentRepository;
    @Autowired CommunityBoardLikeRepository likeRepository;
    @Autowired InquiryRepository inquiryRepository;

    private MemberRemover remover;

    @BeforeEach
    void setUp() {
        FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);
        CommunityPostRemover postRemover = new CommunityPostRemover(
                boardRepository, commentRepository, likeRepository, fileStorageService);
        remover = new MemberRemover(memberRepository, boardRepository, commentRepository,
                likeRepository, inquiryRepository, postRemover);
    }

    private Member member(String loginId, Role role) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setPassword("pw");
        m.setUsername("사용자");
        m.setNickname(loginId);
        m.setEmail(loginId + "@test.com");
        m.setRole(role);
        m.setBirthDate(LocalDate.of(1990, 1, 1));
        m.setPhoneNumber("010-0000-0000");
        return memberRepository.save(m);
    }

    private CommunityBoard post(Member author, String title) {
        CommunityBoard b = new CommunityBoard();
        b.setMember(author);
        b.setTitle(title);
        b.setContent("본문");
        return boardRepository.save(b);
    }

    private void comment(CommunityBoard board, Member writer) {
        CommunityBoardComment c = new CommunityBoardComment();
        c.setBoard(board);
        c.setMember(writer);
        c.setContent("댓글");
        commentRepository.save(c);
    }

    private void like(CommunityBoard board, Member who) {
        CommunityBoardLike l = new CommunityBoardLike();
        l.setBoard(board);
        l.setMember(who);
        likeRepository.save(l);
    }

    @Test
    void 글_댓글_좋아요_문의를_남긴_회원이_지워진다() {
        Member target = member("leaver", Role.PARENT);
        Member other = member("other", Role.PARENT);

        CommunityBoard mine = post(target, "내 글");
        comment(mine, other);                    // 남이 내 글에 단 댓글
        like(mine, other);

        CommunityBoard others = post(other, "남의 글");
        comment(others, target);                 // 내가 남의 글에 단 댓글 — 게시글 삭제로는 안 지워진다
        like(others, target);

        Inquiry inquiry = new Inquiry();
        inquiry.setMember(target);
        inquiry.setTitle("문의");
        inquiry.setContent("내용");
        inquiryRepository.save(inquiry);

        assertThatCode(() -> remover.remove(target)).doesNotThrowAnyException();

        assertThat(memberRepository.findById(target.getId())).isEmpty();
        // 남의 글은 남고, 거기 달린 내 댓글·좋아요만 사라진다.
        assertThat(boardRepository.findById(others.getId())).isPresent();
        assertThat(boardRepository.findById(mine.getId())).isEmpty();
        assertThat(inquiryRepository.findById(inquiry.getId())).isEmpty();
    }

    @Test
    void 자녀를_둔_부모가_지워지면_자녀는_남고_부모연결만_끊긴다() {
        Member parent = member("papa", Role.PARENT);
        Member child = member("kid", Role.CHILD);
        child.setParent(parent);
        memberRepository.save(child);

        assertThatCode(() -> remover.remove(parent)).doesNotThrowAnyException();

        Member reloaded = memberRepository.findById(child.getId()).orElseThrow();
        // 부모 탈퇴로 자녀 데이터(위치·지갑·결제이력)가 통째로 사라지면 안 된다(확정 ①).
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getParent()).isNull();
    }

    @Test
    void 관리자_계정은_물리삭제_대상이_아니다() {
        Member admin = member("adm", Role.SUPER_ADMIN);

        assertThat(remover.isRemovable(admin)).isFalse();
        assertThatThrownBy(() -> remover.remove(admin))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(memberRepository.findById(admin.getId())).isPresent();
    }

    @Test
    void 자식이_없는_회원도_그대로_지워진다() {
        Member plain = member("plain", Role.PARENT);

        assertThatCode(() -> remover.remove(plain)).doesNotThrowAnyException();
        assertThat(memberRepository.findById(plain.getId())).isEmpty();
    }
}
