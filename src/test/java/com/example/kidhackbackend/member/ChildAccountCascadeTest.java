package com.example.kidhackbackend.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.member.service.ChildAccountCascade;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/** #194(QA #26) — 부모가 없어질 때 자녀도 isDeleted 로 함께 정리되는지. */
@DataJpaTest
@Import({ChildAccountCascade.class, JpaAuditingConfig.class})
class ChildAccountCascadeTest {

    @Autowired ChildAccountCascade cascade;
    @Autowired MemberRepository memberRepository;

    private Member member(String loginId, Role role, Member parent) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setUsername("이름");
        m.setNickname(loginId);
        m.setPassword("pw");
        m.setPhoneNumber("01000000000");
        m.setBirthDate(LocalDate.of(2014, 1, 1));
        m.setRole(role);
        if (parent != null) m.setParent(parent);
        return memberRepository.save(m);
    }

    @Test
    void 부모의_자녀들이_모두_isDeleted_된다() {
        Member parent = member("p1", Role.PARENT, null);
        Member c1 = member("c1", Role.CHILD, parent);
        Member c2 = member("c2", Role.CHILD, parent);

        cascade.deactivateChildrenOf(parent.getId());

        assertThat(memberRepository.findById(c1.getId()).orElseThrow().isDeleted()).isTrue();
        assertThat(memberRepository.findById(c2.getId()).orElseThrow().isDeleted()).isTrue();
    }

    @Test
    void 남의_자녀는_건드리지_않는다() {
        Member p1 = member("p1", Role.PARENT, null);
        Member p2 = member("p2", Role.PARENT, null);
        Member other = member("c_other", Role.CHILD, p2);

        cascade.deactivateChildrenOf(p1.getId());

        assertThat(memberRepository.findById(other.getId()).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    void 자녀가_없으면_아무_일도_없다() {
        Member lone = member("lone", Role.PARENT, null);
        cascade.deactivateChildrenOf(lone.getId());   // 예외 없이 통과
        assertThat(memberRepository.findById(lone.getId()).orElseThrow().isDeleted()).isFalse();
    }
}
