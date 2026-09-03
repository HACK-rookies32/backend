package com.example.kidhackbackend.users.member.service;

import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardCommentRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardLikeRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardRepository;
import com.example.kidhackbackend.users.communityboard.service.CommunityPostRemover;
import com.example.kidhackbackend.users.inquiry.repository.InquiryRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 회원 하나를 자식까지 정리하며 지운다.
 *
 * <p>{@code Member} 를 <b>필수 FK({@code nullable = false})로 참조하는 자식이 6종</b>인데
 * cascade 도 {@code ON DELETE} 도 없다. 그래서 회원 행만 지우려 하면 FK 에 걸려
 * {@code DataIntegrityViolationException} 이 난다.
 *
 * <pre>
 *   CommunityBoard.member · CommunityBoardComment.member · CommunityBoardLike.member
 *   Inquiry.member · Notice.author · Member.parent(자기참조)
 * </pre>
 *
 * <p>정리 순서를 여기 한 곳에 모아 둔다 — {@code CommunityPostRemover} 와 같은 이유다.
 * 지우는 경로가 늘어나도 각자 정리하게 두면 한쪽만 고쳐져 다시 어긋난다.
 */
@Component
@RequiredArgsConstructor
public class MemberRemover {

    /**
     * 관리자 계정은 물리 삭제하지 않는다(확정 ②).
     *
     * <p>{@code Notice.author} 가 필수 FK 라 공지를 쓴 관리자를 지우려면 공지를 지우거나
     * 작성자를 옮겨야 하는데, 어느 쪽도 이력이 훼손된다. 관리자 계정을 물리적으로 없애야 할
     * 이유가 없어 {@code isDeleted} 상태로만 남긴다.
     */
    private static final Set<Role> ADMIN_ROLES = Set.of(
            Role.SUPER_ADMIN, Role.GENERAL_ADMIN, Role.COMMUNITY_ADMIN, Role.INQUIRY_ADMIN);

    private final MemberRepository memberRepository;
    private final CommunityBoardRepository boardRepository;
    private final CommunityBoardCommentRepository commentRepository;
    private final CommunityBoardLikeRepository likeRepository;
    private final InquiryRepository inquiryRepository;
    private final CommunityPostRemover postRemover;

    /** 물리 삭제 대상인지. 관리자 계정은 제외한다. */
    public boolean isRemovable(Member member) {
        return !ADMIN_ROLES.contains(member.getRole());
    }

    /**
     * 회원과 딸린 것들을 지운다.
     *
     * <p>순서가 중요하다. 본인 게시글을 {@link CommunityPostRemover} 로 지우면 그 글의 댓글·
     * 좋아요·첨부가 함께 사라지지만, <b>남의 글에 단 댓글·좋아요는 그 경로로 정리되지 않는다.</b>
     * 그래서 회원 기준으로 한 번 더 지운다.
     *
     * @throws IllegalArgumentException 관리자 계정을 넘긴 경우
     */
    @Transactional
    public void remove(Member member) {
        if (!isRemovable(member)) {
            throw new IllegalArgumentException("관리자 계정은 물리 삭제 대상이 아닙니다: " + member.getLoginId());
        }
        Long memberId = member.getId();

        // ① 자녀의 부모 연결을 끊는다. (확정 ① 폐기, QA #26): 예전엔 자녀를 보존했으나
        //    이제 부모 탈퇴/삭제 시 자녀도 ChildAccountCascade 가 isDeleted 로 찍어 함께 정리한다.
        //    자녀는 자신의 isDeleted 로 이 배치가 별도로 물리삭제하므로, 여기서는 부모 삭제를 위한
        //    FK 해제(연결 끊기)만 한다 — 아직 안 지워진 자녀가 부모를 FK 로 물고 있으면 부모 삭제가 막힌다.
        List<Member> children = memberRepository.findByParentId(memberId);
        for (Member child : children) {
            child.setParent(null);
        }
        memberRepository.saveAll(children);

        // ② 본인 게시글 — 딸린 댓글·좋아요·첨부까지 함께 정리된다(#157).
        boardRepository.findByMemberId(memberId, org.springframework.data.domain.Pageable.unpaged())
                .forEach(postRemover::remove);

        // ③ 남의 글에 남긴 댓글·좋아요 — ②로는 정리되지 않는다.
        commentRepository.deleteByMemberId(memberId);
        likeRepository.deleteByMemberId(memberId);

        // ④ 본인 문의
        inquiryRepository.deleteByMemberId(memberId);

        memberRepository.delete(member);
    }
}
