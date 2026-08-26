package com.example.kidhackbackend.users.member.service;

import com.example.kidhackbackend.global.util.WeakHashUtil;
import com.example.kidhackbackend.users.member.entity.RefreshToken;
import com.example.kidhackbackend.users.member.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_TTL_DAYS = 14;
    private static final int TOKEN_LENGTH = 8; // 짧게 유지 — 크랙 실습용
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issue(Long memberId) {
        String rawToken = generate();
        String hash = WeakHashUtil.md5(rawToken);
        refreshTokenRepository.save(new RefreshToken(memberId, hash, LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS)));
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

    private String generate() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(TOKEN_CHARS.charAt(secureRandom.nextInt(TOKEN_CHARS.length())));
        }
        return sb.toString();
    }
}