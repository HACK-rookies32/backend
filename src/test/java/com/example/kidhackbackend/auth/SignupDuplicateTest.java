package com.example.kidhackbackend.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.kidhackbackend.global.exception.DuplicateResourceException;
import com.example.kidhackbackend.global.jwt.JwtUtil;
import com.example.kidhackbackend.users.member.dto.SignupRequest;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.member.service.AuthService;
import com.example.kidhackbackend.users.member.service.RefreshTokenService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 가입 시 중복 검사.
 *
 * <p>이메일에 unique 제약이 걸려 있는데 가입 경로에는 확인이 없어서, 남이 쓰는 주소로
 * 가입하면 flush 시점에 DB 예외가 나 <b>500</b> 이 됐다. 사용자에게는 "서버 오류가
 * 발생했습니다" 만 보여 원인을 알 수 없었다.
 *
 * <p>관리자 회원수정·회원용 프로필 수정은 같은 상황에서 409 를 준다. 가입도 맞춘다.
 */
class SignupDuplicateTest {

    private MemberRepository memberRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        authService = new AuthService(
                memberRepository, mock(JwtUtil.class), mock(RefreshTokenService.class));
        // 기본은 "중복 없음". 각 테스트가 필요한 것만 덮어쓴다.
        when(memberRepository.existsByLoginId(any())).thenReturn(false);
        when(memberRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private SignupRequest request(String loginId, String email, Role role) {
        SignupRequest r = new SignupRequest();
        ReflectionTestUtils.setField(r, "loginId", loginId);
        ReflectionTestUtils.setField(r, "username", "홍길동");
        ReflectionTestUtils.setField(r, "password", "pw1234!");
        ReflectionTestUtils.setField(r, "nickname", "닉네임");
        ReflectionTestUtils.setField(r, "email", email);
        ReflectionTestUtils.setField(r, "phoneNumber", "010-1111-2222");
        ReflectionTestUtils.setField(r, "birthDate", LocalDate.of(2016, 1, 1));
        ReflectionTestUtils.setField(r, "role", role);
        return r;
    }

    @Test
    void 이미_쓰이는_이메일이면_409_로_막는다() {
        when(memberRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new Member()));

        assertThatThrownBy(() -> authService.signup(request("newbie", "taken@test.com", Role.PARENT)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        // DB 까지 가면 안 된다 — 거기서 터지면 500 이 된다.
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void 아이디_중복은_그대로_막는다() {
        when(memberRepository.existsByLoginId("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request("taken", "new@test.com", Role.PARENT)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    /** 자녀는 이메일 없이 가입할 수 있다. 빈 값에 중복 검사를 걸면 두 번째 자녀부터 막힌다. */
    @Test
    void 자녀는_이메일_없이_가입된다() {
        assertThatCode(() -> authService.signup(request("child2", null, Role.CHILD)))
                .doesNotThrowAnyException();

        verify(memberRepository, never()).findByEmail(any());
    }

    @Test
    void 새_이메일이면_가입된다() {
        assertThatCode(() -> authService.signup(request("newbie", "fresh@test.com", Role.PARENT)))
                .doesNotThrowAnyException();

        verify(memberRepository).save(any(Member.class));
    }
}
