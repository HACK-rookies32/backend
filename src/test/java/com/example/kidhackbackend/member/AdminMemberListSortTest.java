package com.example.kidhackbackend.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.admin.member.dto.MemberSort;
import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

/** #173 로 추가·변경된 것만 확인한다 — 정렬 드롭다운 4종, 탈퇴 회원 제외. */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class AdminMemberListSortTest {

    @Autowired MemberRepository memberRepository;

    /** 가입 순서와 이름 순서가 어긋나게 만든다 — 정렬 기준이 실제로 갈리는지 보려면 필요하다. */
    private Member member(String loginId, String username) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setUsername(username);
        m.setNickname(loginId);
        m.setPassword("pw");
        m.setEmail(loginId + "@test.com");
        m.setPhoneNumber("01000000000");
        m.setBirthDate(LocalDate.of(1990, 1, 1));
        m.setRole(Role.PARENT);
        return memberRepository.save(m);
    }

    private List<String> usernames(MemberSort sort) {
        return memberRepository.searchCustomers(null, null, PageRequest.of(0, 10, sort.toSort()))
                .getContent().stream().map(Member::getUsername).toList();
    }

    private void seedFour() {
        member("u1", "최민수");
        member("u2", "강하늘");
        member("u3", "박지훈");
        member("u4", "안유진");
    }

    @Test
    void 정렬_기준별로_순서가_달라진다() {
        seedFour();

        assertThat(usernames(MemberSort.RECENT)).containsExactly("안유진", "박지훈", "강하늘", "최민수");
        assertThat(usernames(MemberSort.OLDEST)).containsExactly("최민수", "강하늘", "박지훈", "안유진");
        assertThat(usernames(MemberSort.NAME)).containsExactly("강하늘", "박지훈", "안유진", "최민수");
        assertThat(usernames(MemberSort.NAME_DESC)).containsExactly("최민수", "안유진", "박지훈", "강하늘");
    }

    @Test
    void 탈퇴_회원은_목록에서_빠진다() {
        seedFour();
        Member gone = memberRepository.findByLoginId("u3").orElseThrow();
        gone.setDeleted(true);
        memberRepository.save(gone);

        assertThat(usernames(MemberSort.RECENT)).containsExactly("안유진", "강하늘", "최민수");
        assertThat(memberRepository.searchWalletsWithBalance(null, null, PageRequest.of(0, 10)))
                .extracting(p -> p.getUserName())
                .doesNotContain("박지훈");
    }
}
