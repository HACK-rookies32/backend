package com.example.kidhackbackend.users.member.repository;

import com.example.kidhackbackend.users.member.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    long countByMemberId(Long memberId); // [VULN-AUTH-RT3] 자녀별 발급 순번(N) 계산용
    void deleteByMemberId(Long memberId);
}