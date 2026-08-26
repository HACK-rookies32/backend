package com.example.kidhackbackend.users.qrpay.security;

import com.example.kidhackbackend.global.jwt.JwtAuthenticationFilterGio;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * QR결제 도메인 전용 시큐리티 — 새 도메인이라 {@code @Order}는 wallet(7)/pg웹훅(8) 다음인 9부터.
 *
 * <p>두 체인으로 나눈다:
 * <ul>
 *   <li><b>매장 단말 체인</b>(@Order 9, {@code /api/qrpay/terminal/**}): PG 웹훅처럼 permitAll —
 *       앱 JWT가 아니라 {@code X-Store-Terminal-Token} 검증으로 진위를 담보(No.44 결제 승인 경로).</li>
 *   <li><b>앱 체인</b>(@Order 10, 그 외 {@code /api/qrpay/**}): 공통 JWT 필터로 인증만 요구.
 *       단말 발급(No.44 선행)·QR 발급/폴링/내역(No.42·43·45) 등 앱 사용자용.</li>
 * </ul>
 *
 * <p><b>주의(모의해킹):</b> 단말 발급은 관리자 전용이어야 하지만 앱 체인은 '인증만'(authenticated) 요구한다.
 * 역할 제한이 컨트롤러에도 없어(권한상승 취약점) 일반 유저가 단말 토큰을 발급받을 수 있다
 * (see {@code StoreTerminalController} [VULN-WALLET-11]).
 */
@Configuration
public class QrPaySecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilter;

    public QrPaySecurityConfig(JwtAuthenticationFilterGio jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /** 매장 단말 경로: permitAll(진위는 X-Store-Terminal-Token 검증). */
    @Bean
    @Order(9)
    public SecurityFilterChain qrTerminalSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/qrpay/terminal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll());

        return http.build();
    }

    /** QR결제 앱 경로: 로그인(인증)만 요구. */
    @Bean
    @Order(10)
    public SecurityFilterChain qrPayAppSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/qrpay/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다."))
                        .accessDeniedHandler((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다.")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
