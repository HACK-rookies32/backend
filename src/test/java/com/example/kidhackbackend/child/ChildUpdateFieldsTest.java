package com.example.kidhackbackend.child;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.global.exception.DuplicateResourceException;
import com.example.kidhackbackend.users.child.dto.ChildUpdateRequest;
import com.example.kidhackbackend.users.child.service.ChildService;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.location.service.TrackingSettingService;
import com.example.kidhackbackend.users.member.service.RefreshTokenService;
import com.example.kidhackbackend.global.util.WeakHashUtil;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

/** #196(QA #27) — 자녀 수정에 추가된 6필드가 반영되는지 + email 중복은 409. */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class ChildUpdateFieldsTest {

    @Autowired MemberRepository memberRepository;
    private ChildService service;

    @BeforeEach
    void setUp() {
        service = new ChildService(
                memberRepository,
                Mockito.mock(RefreshTokenService.class),
                Mockito.mock(TrackingSettingService.class));
    }

    private Member child(String loginId, String email) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setUsername("원래이름");
        m.setNickname(loginId);
        m.setPassword("old");
        m.setEmail(email);
        m.setPhoneNumber("010-0000-0000");
        m.setBirthDate(LocalDate.of(2015, 1, 1));
        m.setRole(Role.CHILD);
        return memberRepository.save(m);
    }

    private ChildUpdateRequest req(java.util.function.Consumer<ChildUpdateRequest> c) {
        ChildUpdateRequest r = new ChildUpdateRequest();
        c.accept(r);
        return r;
    }

    @Test
    void 추가된_6필드가_모두_반영된다() {
        Member c = child("c1", "c1@t.com");
        ChildUpdateRequest r = req(x -> {
            ReflectionTestUtils.setField(x, "username", "새이름");
            ReflectionTestUtils.setField(x, "email", "new@t.com");
            ReflectionTestUtils.setField(x, "phoneNumber", "010-9999-8888");
            ReflectionTestUtils.setField(x, "birthDate", LocalDate.of(2016, 5, 5));
            ReflectionTestUtils.setField(x, "address", "서울시 강남구");
            ReflectionTestUtils.setField(x, "password", "newpw");
        });

        service.updateChild(c.getId(), r);

        Member saved = memberRepository.findById(c.getId()).orElseThrow();
        assertThat(saved.getUsername()).isEqualTo("새이름");
        assertThat(saved.getEmail()).isEqualTo("new@t.com");
        assertThat(saved.getPhoneNumber()).isEqualTo("010-9999-8888");
        assertThat(saved.getBirthDate()).isEqualTo(LocalDate.of(2016, 5, 5));
        assertThat(saved.getAddress()).isEqualTo("서울시 강남구");
        assertThat(saved.getPassword()).isEqualTo(WeakHashUtil.sha256("newpw")); // salt 없는 SHA-256(VULN-1)로 저장
    }

    @Test
    void non_null_필드만_반영하고_나머지는_유지() {
        Member c = child("c2", "c2@t.com");
        service.updateChild(c.getId(), req(x -> ReflectionTestUtils.setField(x, "address", "부산시")));

        Member saved = memberRepository.findById(c.getId()).orElseThrow();
        assertThat(saved.getAddress()).isEqualTo("부산시");
        assertThat(saved.getUsername()).isEqualTo("원래이름"); // 안 보낸 필드 유지
        assertThat(saved.getEmail()).isEqualTo("c2@t.com");
    }

    @Test
    void 다른_회원과_email_중복이면_409_DuplicateResource() {
        child("owner", "taken@t.com");
        Member c = child("c3", "c3@t.com");

        assertThatThrownBy(() -> service.updateChild(c.getId(),
                req(x -> ReflectionTestUtils.setField(x, "email", "taken@t.com"))))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
