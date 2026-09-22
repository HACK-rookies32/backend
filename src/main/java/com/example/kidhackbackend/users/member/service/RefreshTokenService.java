package com.example.kidhackbackend.users.member.service;

import com.example.kidhackbackend.global.util.WeakHashUtil;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.RefreshToken;
import com.example.kidhackbackend.users.member.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_TTL_DAYS = 14;

    private final RefreshTokenRepository refreshTokenRepository;

    // [VULN-AUTH-RT3] refresh token 을 랜덤이 아니라 "{memberId}:{loginId}:{N}" (N=자녀별 발급 순번)
    //   을 표준 Base64 로 인코딩해 만든다. memberId·loginId 는 자녀관리 IDOR(GET /api/children/{id},
    //   [VULN-CHILD-*])로 그대로 노출되고, N 은 1,2,3… 으로 열거되므로 공격자가 피해자 자녀의 refresh
    //   token 을 유추(재구성)할 수 있다. 비밀키·서명이 없어 노출값만으로 재구성되지만,
    //   verify()가 stateful(md5 조회)이라 "무에서의 순수 위조"는 아니고, 피해자가 실제 로그인해
    //   발급받은 유효 row 가 있어야 통과한다(= 이미 발급된 토큰의 예측/재구성).
    @Transactional
    public String issue(Member member) {
        long seq = refreshTokenRepository.countByMemberId(member.getId()) + 1;
        String rawToken = generate(member.getId(), member.getLoginId(), seq);
        String hash = WeakHashUtil.md5(rawToken);
        refreshTokenRepository.save(new RefreshToken(member.getId(), hash, LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS)));
        return rawToken;
    }

    // [VULN-AUTH-RT2] 재발급해도 기존 토큰을 무효화하지 않음(로테이션 없음) — 탈취된 토큰이 만료 전까지 계속 유효
    @Transactional(readOnly = true)
    public Long verify(String rawToken) {
        String hash = WeakHashUtil.md5(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 refresh token입니다."));
        if (token.isExpired()) {
            throw new IllegalArgumentException("만료된 refresh token입니다.");
        }
        return token.getMemberId();
    }

    @Transactional
    public void revokeAll(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }

    // [VULN-AUTH-RT3] 예측 가능한 refresh token 생성: Base64("{memberId}:{loginId}:{순번}")
    private String generate(Long memberId, String loginId, long seq) {
        String raw = memberId + ":" + loginId + ":" + seq;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
