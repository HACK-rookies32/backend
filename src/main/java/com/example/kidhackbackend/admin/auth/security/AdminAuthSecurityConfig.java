package com.example.kidhackbackend.admin.auth.security;

import com.example.kidhackbackend.global.exception.JwtAccessDeniedHandler;
import com.example.kidhackbackend.global.exception.JwtAuthenticationEntryPoint;
import com.example.kidhackbackend.global.jwt.JwtAuthenticationFilterGio;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 관리자 인증 전용 시큐리티 체인 (/api/admin/auth/**).
 *
 * <p><b>왜 필요한가.</b> 다른 admin 체인들은 {@code members}·{@code community}·{@code notices} 처럼
 * 각자 경로만 {@code securityMatcher} 로 잡는다. {@code /api/admin/auth/**} 는 어디에도 걸리지 않아
 * 전역 {@code SecurityConfig} 의 {@code anyRequest().permitAll()} 로 떨어진다 — 로그인이 토큰 없이
 * 동작하던 이유다. 이 상태에서 로그아웃을 추가하면 <b>인증 없이 아무나 호출</b>할 수 있으므로,
 * 이 체인으로 로그인만 열고 로그아웃은 관리자 인증을 요구한다.
 *
 * <p>다른 admin 체인과 겹치지 않게 {@code @Order(15)} (1~14 사용 중, securityMatcher 가 서로 배타적이라
 * 순서 의존성은 없다). 토큰 없음은 401, 역할 불일치는 403 으로 나간다.
 */
@Configuration
@RequiredArgsConstructor
public class AdminAuthSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilterGio;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    @Order(15)
    public SecurityFilterChain adminAuthFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/admin/auth/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 로그인은 토큰 없이 호출해야 한다.
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/login").permitAll()
                        // 로그아웃은 관리자 4종 모두 가능(자기 세션 종료는 권한이 필요한 작업이 아니다).
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/logout")
                        .hasAnyRole("SUPER_ADMIN", "GENERAL_ADMIN", "COMMUNITY_ADMIN", "INQUIRY_ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilterGio, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler));
        return http.build();
    }
}
