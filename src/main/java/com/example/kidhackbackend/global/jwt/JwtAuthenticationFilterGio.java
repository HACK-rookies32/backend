package com.example.kidhackbackend.global.jwt;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 공통 JWT 인증 필터. (기존 JwtAuthenticationFilterGio 유지, 이름 변경 없음 — 다른 도메인 참조 그대로 유효)
 * {@code Authorization: Bearer <토큰>} 헤더의 토큰을 인증 도메인이 발급한 시크릿({@code jwt.secret})으로
 * 검증하고, subject(username)와 role 클레임을 Spring Security 인증 객체로 세팅한다.
 * 토큰이 없거나 유효하지 않으면 인증 객체를 세팅하지 않아 이후 인가 단계에서 401/403으로 처리된다.
 *
 * 각 도메인의 시큐리티 체인(예: 위치 도메인)이 이 필터를 등록해 재사용한다.
 */
public class JwtAuthenticationFilterGio extends OncePerRequestFilter {

    private final SecretKey signingKey;
    private final MemberRepository memberRepository;

    public JwtAuthenticationFilterGio(String secret, MemberRepository memberRepository) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                // [VULN-LOC-2] 권한을 토큰의 role 클레임만으로 부여하고 DB와 대조하지 않는다.
                //   - 시크릿이 하드코딩/노출돼 있어([VULN-2]) 시크릿을 아는 공격자가 role=PARENT/ADMIN
                //     토큰을 위조하면 서명 검증을 통과해 그대로 권한 상승된다.
                //   - 서버측 role 재검증이 없어([VULN-5]) 탈취·변경된 role이 만료 전까지 계속 유효하다.
                //     (#202 로 '자녀 강제 로그아웃' 만 예외적으로 발급분을 무효화한다 — 아래 tokensValidAfter
                //      비교. 그 외 계정·일반 로그아웃·위조 토큰은 여전히 무효화되지 않는다.)
                if (username != null && role != null) {
                    var authority = new SimpleGrantedAuthority("ROLE_" + role);
                    Object principal = memberRepository.findByLoginId(username)
                            .<Object>map(m -> m)
                            .orElse(username);

                    // [#202] 강제 로그아웃 반영: tokensValidAfter 이후 발급된 토큰만 유효로 본다.
                    //   부모의 자녀 강제 로그아웃에서만 이 값이 세팅되므로, 그 외 계정(값=null)은
                    //   그대로 통과한다 — VULN-LOC-2/VULN-5(무상태·role 무검증·위조)는 유지된다.
                    //   JWT iat 는 초 단위라 초 경계로 비교한다.
                    if (principal instanceof Member m && m.getTokensValidAfter() != null) {
                        Date iat = claims.getIssuedAt();
                        long iatSec = (iat != null) ? iat.getTime() / 1000L : 0L;
                        long validAfterSec = m.getTokensValidAfter()
                                .atZone(ZoneId.systemDefault()).toEpochSecond();
                        if (iatSec < validAfterSec) {
                            SecurityContextHolder.clearContext();
                            filterChain.doFilter(request, response);
                            return; // 무효화된 토큰 — 인증 세팅 없이 통과시켜 이후 단계에서 401 처리
                        }
                    }

                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
    /** Authorization 헤더에서 Bearer 토큰만 추출. */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}