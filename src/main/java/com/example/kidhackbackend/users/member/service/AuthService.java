package com.example.kidhackbackend.users.member.service;

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
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        Member member = new Member();
        member.setUsername(request.getUsername());
        member.setPassword(request.getPassword()); // [VULN-1] 평문 저장
        member.setNickname(request.getNickname());
        member.setEmail(request.getEmail());
        member.setRole(request.getRole()); // [VULN-7] 클라이언트가 보낸 role을 그대로 신뢰
        member.setPhoneNumber(request.getPhoneNumber());
        member.setBirthDate(request.getBirthDate());
        member.setAddress(request.getAddress());
        member.setProfileImageUrl(request.getProfileImageUrl());

        if (request.getRole() == Role.CHILD && request.getParentId() != null) {
            // [VULN-8] parentId가 실제 존재하는지, 그 계정이 진짜 PARENT인지,
            // 본인이 그 부모 밑에 등록될 자격이 있는지 전혀 검증하지 않음
            // → id가 순차 증가라 남의 부모 id를 아무거나 넣어도 그대로 연결됨 (IDOR)
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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (!member.getPassword().equals(request.getPassword())) {
            // [VULN-3] 비밀번호 틀림도 아이디 없음과 다른 메시지로 응답
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // [VULN-4] 로그인 실패 횟수 제한/계정 잠금이 없어서 무제한 브루트포스 가능

        String token = jwtUtil.generateToken(member.getUsername(), member.getRole());
        return new LoginResponse(token, member.getUsername(), member.getRole());
    }
}