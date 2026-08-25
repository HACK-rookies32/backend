package com.example.kidhackbackend.users.member.service;

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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    public SignupResponse signup(SignupRequest request) {
        // [VULN-6] 동시 요청 시 중복 체크와 저장 사이에 경쟁 조건 존재
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("이미 존재하는 아이디입니다.");
        }

        // CHILD가 아니면 이메일 필수
        if (request.getRole() != Role.CHILD
                && (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }

        Member member = new Member();
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
        Member member = memberRepository.findByUsername(request.getUsername())
                // [VULN-3] 아이디가 없다는 걸 명확히 알려줌 -> 계정 존재 여부(사용자 열거) 노출
                .orElseThrow(() -> new InvalidCredentialException("존재하지 않는 아이디입니다."));

        if (!member.getPassword().equals(request.getPassword())) {
            // [VULN-3] 비밀번호 틀림도 아이디 없음과 다른 메시지로 응답
            throw new InvalidCredentialException("비밀번호가 일치하지 않습니다.");
        }

        // [VULN-4] 로그인 실패 횟수 제한/계정 잠금이 없어서 무제한 브루트포스 가능

        String token = jwtUtil.generateToken(member.getUsername(), member.getRole());
        return new LoginResponse(token, member.getUsername(), member.getRole());
    }
}