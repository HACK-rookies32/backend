package com.example.kidhackbackend.users.wallet.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PG 웹훅 전용 시큐리티 체인 (/api/pg/**) — 역할표 No.36.
 *
 * <p>PG 서버가 호출하는 서버-투-서버 콜백이므로 앱 JWT 인증을 걸지 않고 <b>permitAll</b>로 열되,
 * 진위는 웹훅 처리 단계의 <b>서명 검증</b>으로 담보한다. (앱용 /api/wallet PARENT 체인과 경로가 겹치지 않음)
 * {@code @Order(8)} — wallet(7) 다음.
 */
@Configuration
public class PgWebhookSecurityConfig {

    @Bean
    @Order(8)
    public SecurityFilterChain pgWebhookSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/pg/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll());

        return http.build();
    }
}
