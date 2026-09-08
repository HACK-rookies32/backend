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
 * <p>자금 조회/변경은 SUPER_ADMIN 전용이다. 기존 admin 체인들과 겹치지 않게 {@code @Order(11)}.
 * (이전에는 GENERAL_ADMIN 도 허용해 강제환불·잔액조정 No.48·50 이 컨트롤러에서 걸러지지 않는
 *  권한 취약점 {@code [VULN-ADMIN-1]} 이 있었으나, 권한표(자금관리=SUPER_ADMIN 전용)에 맞춰 체인에서 닫았다.)
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
                        // 자금 조회/변경은 SUPER_ADMIN 전용. GENERAL_ADMIN 접근을 닫아 권한표와 일치시킨다.
                        .anyRequest().hasRole("SUPER_ADMIN"))
                .addFilterBefore(jwtAuthenticationFilterGio, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler));
        return http.build();
    }
}
