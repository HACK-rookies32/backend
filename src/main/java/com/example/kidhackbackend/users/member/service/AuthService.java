package com.example.kidhackbackend.users.member.service;

import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
import com.example.kidhackbackend.global.exception.DuplicateResourceException;
import com.example.kidhackbackend.global.exception.InvalidCredentialException;
import com.example.kidhackbackend.global.jwt.JwtUtil;
import com.example.kidhackbackend.users.member.dto.LoginRequest;
import com.example.kidhackbackend.users.member.dto.LoginResponse;
import com.example.kidhackbackend.users.member.dto.SignupRequest;
import com.example.kidhackbackend.users.member.dto.SignupResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.kidhackbackend.users.member.dto.TokenRefreshRequest;
import com.example.kidhackbackend.users.member.dto.TokenRefreshResponse;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    private static final Set<Role> ADMIN_ROLES = Set.of(
            Role.SUPER_ADMIN, Role.GENERAL_ADMIN, Role.COMMUNITY_ADMIN, Role.INQUIRY_ADMIN
    );

    public SignupResponse signup(SignupRequest request) {
        // [VULN-6] 동시 요청 시 중복 체크와 저장 사이에 경쟁 조건 존재
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            throw new DuplicateResourceException("이미 존재하는 아이디입니다.");
        }

        // CHILD가 아니면 이메일 필수
        if (request.getRole() != Role.CHILD
                && (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }

        // 이메일도 unique 라, 남이 쓰는 주소를 그대로 저장하면 flush 시점에 제약 위반으로 500 이 났다.
        // 사용자에게는 "서버 오류가 발생했습니다" 만 보여 원인을 알 수 없었다.
        // 관리자 회원수정·회원용 PATCH /api/users/me 는 같은 상황에서 409 를 준다 — 문구까지 맞춘다.
        // 자녀는 이메일 없이 가입할 수 있으므로 비어 있으면 검사하지 않는다.
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && memberRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("이미 사용 중인 이메일입니다.");
        }

        Member member = new Member();
        member.setLoginId(request.getLoginId());
        member.setUsername(request.getUsername());
        member.setPassword(request.getPassword()); // [VULN-1] 평문 저장
        member.setNickname(request.getNickname());
        member.setEmail(request.getEmail());
        member.setPhoneNumber(request.getPhoneNumber());
        member.setBirthDate(request.getBirthDate());
        member.setAddress(request.getAddress());
        member.setProfileImageUrl(request.getProfileImageUrl());
        member.setRole(request.getRole()); // [VULN-7] 클라이언트가 보낸 role을 그대로 신뢰

        if (request.getRole() == Role.CHILD && request.getParentId() != null) {
            // [VULN-8] parentId 검증 없음 (IDOR)
            Member parent = memberRepository.findById(request.getParentId()).orElse(null);
            member.setParent(parent);
        }

        memberRepository.save(member);

        return new SignupResponse(
                member.getId(),
                member.getLoginId(),
                member.getUsername(),
                member.getNickname(),
                member.getEmail(),
                member.getPhoneNumber(),
                member.getBirthDate(),
                member.getAddress(),
                member.getProfileImageUrl(),
                member.getRole(),
                member.getParent() != null ? member.getParent().getId() : null,
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByLoginId(request.getLoginId())
                // [VULN-3] 아이디가 없다는 걸 명확히 알려줌 -> 계정 존재 여부(사용자 열거) 노출
                .orElseThrow(() -> new InvalidCredentialException("존재하지 않는 아이디입니다."));

        if (!member.getPassword().equals(request.getPassword())) {
            // [VULN-3] 비밀번호 틀림도 아이디 없음과 다른 메시지로 응답
            throw new InvalidCredentialException("비밀번호가 일치하지 않습니다.");
        }

        if (member.isDeleted()) {
            throw new InvalidCredentialException("탈퇴한 계정입니다.");
        }

        // [VULN-4] 로그인 실패 횟수 제한/계정 잠금이 없어서 무제한 브루트포스 가능

        String token = jwtUtil.generateToken(member.getLoginId(), member.getRole());
        String refreshToken = refreshTokenService.issue(member.getId());
        return new LoginResponse(token, refreshToken, member.getLoginId(), member.getUsername(), member.getRole());
    }

    public LoginResponse adminLogin(LoginRequest request) {
        Member member = memberRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new InvalidCredentialException("존재하지 않는 아이디입니다.")); // [VULN-3]

        if (!member.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialException("비밀번호가 일치하지 않습니다."); // [VULN-3]
        }

        if (member.isDeleted()) {
            throw new InvalidCredentialException("탈퇴한 계정입니다.");
        }

        if (!ADMIN_ROLES.contains(member.getRole())) {
            throw new AdminAccessDeniedException("관리자 계정이 아닙니다.");
        }

        // [VULN-4] no rate limiting
        String token = jwtUtil.generateToken(member.getLoginId(), member.getRole());
        String refreshToken = refreshTokenService.issue(member.getId());
        return new LoginResponse(token, refreshToken, member.getLoginId(), member.getUsername(), member.getRole());
    }

    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        Long memberId = refreshTokenService.verify(request.getRefreshToken());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        if (member.isDeleted()) {
            throw new IllegalArgumentException("탈퇴한 계정입니다.");
        }
        String newToken = jwtUtil.generateToken(member.getLoginId(), member.getRole());
        return new TokenRefreshResponse(newToken);
    }

    public void logout(Member member) {
        refreshTokenService.revokeAll(member.getId());
    }
}