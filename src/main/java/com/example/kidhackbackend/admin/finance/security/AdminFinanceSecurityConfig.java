package com.example.kidhackbackend.admin.finance.security;

import com.example.kidhackbackend.global.exception.JwtAccessDeniedHandler;
import com.example.kidhackbackend.global.exception.JwtAuthenticationEntryPoint;
import com.example.kidhackbackend.global.jwt.JwtAuthenticationFilterGio;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 관리자 자금관리 전용 시큐리티 체인 (/api/admin/finance/**) — 역할표 No.47·49·51(조회)·48·50(변경).
 *
 * <p>자금 조회/변경은 관리자(SUPER_ADMIN·GENERAL_ADMIN)만 접근한다. 기존 admin 체인들과 겹치지 않게 {@code @Order(11)}.
 * (강제환불·잔액조정 No.48·50은 SUPER_ADMIN 전용이어야 하지만, 본 체인은 GENERAL_ADMIN도 허용 —
 *  그 권한 경계는 컨트롤러에서 걸러야 하며, 걸지 않은 것이 의도적 취약점이 된다. see {@code [VULN-ADMIN-1]})
 */
@Configuration
@RequiredArgsConstructor
public class AdminFinanceSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilterGio;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    @Order(11)
    public SecurityFilterChain adminFinanceFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/admin/finance/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasAnyRole("SUPER_ADMIN", "GENERAL_ADMIN"))
                .addFilterBefore(jwtAuthenticationFilterGio, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler));
        return http.build();
    }
}
