package com.example.kidhackbackend.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.example.kidhackbackend.users.member.entity.Role;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    // [VULN-2] 시크릿 키가 application.yml에 평문으로 하드코딩되어 있고
    //          그 파일이 그대로 깃 레포에 커밋되어 있음 (환경변수/보안레포 미사용)
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, Role role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role.name()) // [VULN-5] role 클레임을 그대로 신뢰 (서버 재검증 없음)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey()) // [VULN-2] 시크릿 키 하드코딩/노출
                .compact();
    }

    // [VULN-5] 토큰 무효화(로그아웃) 로직이 아예 없음
    //          -> 로그아웃해도 발급된 토큰은 만료시간 전까지 계속 유효함
}