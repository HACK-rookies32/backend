package com.example.kidhackbackend.child;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.users.child.service.ChildService;
import com.example.kidhackbackend.users.location.service.TrackingSettingService;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.member.service.RefreshTokenService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/** #202 — 자녀 강제 로그아웃이 refresh 무효화에 더해 access 토큰 무효화 기준시각(tokensValidAfter)까지 세팅하는지. */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class ChildForceLogoutTest {

    @Autowired MemberRepository memberRepository;
    private ChildService service;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = Mockito.mock(RefreshTokenService.class);
        service = new ChildService(
                memberRepository,
                refreshTokenService,
                Mockito.mock(TrackingSettingService.class));
    }

    private Member child() {
        Member m = new Member();
        m.setLoginId("child1");
        m.setUsername("자녀");
        m.setNickname("child1");
        m.setPassword("pw");
        m.setPhoneNumber("010-0000-0000");
        m.setBirthDate(LocalDate.of(2015, 1, 1));
        m.setRole(Role.CHILD);
        return memberRepository.save(m);
    }

    @Test
    void 강제로그아웃은_refresh를_무효화하고_access_무효화_기준시각을_세팅한다() {
        Member c = child();
        assertThat(c.getTokensValidAfter()).isNull();   // 세팅 전엔 제한 없음
        LocalDateTime before = LocalDateTime.now();

        service.logoutChild(c.getId());

        verify(refreshTokenService).revokeAll(c.getId());               // 기존 동작 유지
        Member reloaded = memberRepository.findById(c.getId()).orElseThrow();
        assertThat(reloaded.getTokensValidAfter())
                .isNotNull()
                .isAfterOrEqualTo(before.withNano(0));                  // 지금(이후)로 세팅됨
    }
}
