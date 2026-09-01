package com.example.kidhackbackend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.kidhackbackend.global.jwt.JwtUtil;
import com.example.kidhackbackend.users.member.dto.AvailabilityResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.member.service.AuthService;
import com.example.kidhackbackend.users.member.service.RefreshTokenService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 가입 전 중복 확인.
 *
 * <p>예전에는 마지막 「가입 완료하기」의 409 로만 알 수 있어서, 사용자가 약관·권한 화면까지
 * 다 지나갔다가 첫 화면으로 되돌아와야 했다. 폼 단계에서 미리 물어볼 수 있게 한다.
 */
class SignupAvailabilityTest {

    private MemberRepository memberRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        authService = new AuthService(
                memberRepository, mock(JwtUtil.class), mock(RefreshTokenService.class));
        when(memberRepository.existsByLoginId(any())).thenReturn(false);
        when(memberRepository.findByEmail(any())).thenReturn(Optional.empty());
    }

    @Test
    void 쓰이는_아이디면_false() {
        when(memberRepository.existsByLoginId("taken")).thenReturn(true);

        AvailabilityResponse result = authService.checkAvailability("taken", null);

        assertThat(result.loginIdAvailable()).isFalse();
    }

    @Test
    void 쓰이는_이메일이면_false() {
        when(memberRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new Member()));

        AvailabilityResponse result = authService.checkAvailability(null, "taken@test.com");

        assertThat(result.emailAvailable()).isFalse();
    }

    @Test
    void 비어_있는_값은_확인하지_않는다() {
        AvailabilityResponse result = authService.checkAvailability("  ", "");

        // 안 물어본 것과 "쓸 수 있다"는 다르다. 자녀는 이메일 없이 가입하므로 이 구분이 필요하다.
        assertThat(result.loginIdAvailable()).isNull();
        assertThat(result.emailAvailable()).isNull();
        verify(memberRepository, never()).existsByLoginId(any());
        verify(memberRepository, never()).findByEmail(any());
    }

    @Test
    void 둘_다_비어_있으면_둘_다_확인한다() {
        AvailabilityResponse result = authService.checkAvailability("newbie", "fresh@test.com");

        assertThat(result.loginIdAvailable()).isTrue();
        assertThat(result.emailAvailable()).isTrue();
    }

    /** 앞뒤 공백 때문에 "쓸 수 있다"가 나오면, 정작 가입에서 409 가 난다. */
    @Test
    void 앞뒤_공백은_떼고_확인한다() {
        when(memberRepository.existsByLoginId("taken")).thenReturn(true);

        AvailabilityResponse result = authService.checkAvailability("  taken  ", null);

        assertThat(result.loginIdAvailable()).isFalse();
    }
}
