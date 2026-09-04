package com.example.kidhackbackend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.kidhackbackend.global.jwt.JwtAuthenticationFilterGio;
import com.example.kidhackbackend.global.jwt.JwtUtil;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * #202 — 공유 JWT 필터가 강제 로그아웃 기준시각(tokensValidAfter)에 따라 access 토큰을 무효화하는지.
 *
 * <p>필터가 "인증을 세팅했는가"로 판정한다 — 무효화되면 인증 객체가 없어 이후 단계에서 401 이 된다.
 */
class JwtForceLogoutInvalidationTest {

    private static final String SECRET = "test-secret-key-0123456789-abcdefghijklmnop"; // HS256 32바이트+
    private static final String LOGIN_ID = "child1";

    private final MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
    private JwtUtil jwtUtil;
    private JwtAuthenticationFilterGio filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L);
        filter = new JwtAuthenticationFilterGio(SECRET, memberRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Member child(LocalDateTime validAfter) {
        Member m = new Member();
        m.setLoginId(LOGIN_ID);
        m.setRole(Role.CHILD);
        m.setTokensValidAfter(validAfter);
        return m;
    }

    private boolean authenticatedAfterFilter() throws Exception {
        String token = jwtUtil.generateToken(LOGIN_ID, Role.CHILD);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Test
    void 기준시각_이전_발급_토큰은_인증되지_않는다() throws Exception {
        // validAfter 가 미래 → 방금 발급한(=그 이전) 토큰은 무효
        when(memberRepository.findByLoginId(LOGIN_ID))
                .thenReturn(Optional.of(child(LocalDateTime.now().plusSeconds(10))));

        assertThat(authenticatedAfterFilter()).isFalse();
    }

    @Test
    void 기준시각_이후_발급_토큰은_정상_인증된다() throws Exception {
        // validAfter 가 과거 → 지금 발급한 토큰은 유효(자녀 재로그인 시나리오)
        when(memberRepository.findByLoginId(LOGIN_ID))
                .thenReturn(Optional.of(child(LocalDateTime.now().minusSeconds(10))));

        assertThat(authenticatedAfterFilter()).isTrue();
    }

    @Test
    void validAfter가_null이면_항상_통과한다_의도된_무상태_유지() throws Exception {
        // 강제 로그아웃된 적 없는 계정 — 기존 무상태 동작 그대로
        when(memberRepository.findByLoginId(LOGIN_ID))
                .thenReturn(Optional.of(child(null)));

        assertThat(authenticatedAfterFilter()).isTrue();
    }
}
