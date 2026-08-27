package com.example.kidhackbackend.admin.audit.security;

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
 * 감사로그 조회 전용 시큐리티 체인 (/api/admin/audit-logs/**).
 *
 * <p>활동 이력은 다른 관리자가 무엇을 했는지 전부 드러내므로 SUPER_ADMIN 만 본다. 다른 admin 체인과
 * 겹치지 않게 {@code @Order(14)} (securityMatcher 가 서로 배타적이라 순서 의존성은 없다).
 * 토큰 없음은 401 UNAUTHORIZED, 권한 부족은 403 FORBIDDEN 으로 나간다.
 */
@Configuration
@RequiredArgsConstructor
public class AdminAuditLogSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilterGio;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    @Order(14)
    public SecurityFilterChain adminAuditLogFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/admin/audit-logs/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SUPER_ADMIN"))
                .addFilterBefore(jwtAuthenticationFilterGio, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler));
        return http.build();
    }
}
