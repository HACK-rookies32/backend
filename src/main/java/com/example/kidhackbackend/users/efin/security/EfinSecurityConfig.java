package com.example.kidhackbackend.users.efin.security;

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
 * 전자금융(efin) 도메인 전용 시큐리티 체인 (/api/efin/**).
 *
 * <p>거래 OTP·계좌비밀번호·실명확인은 모두 로그인 본인이 수행한다(PARENT·CHILD 공통). 공통
 * {@link JwtAuthenticationFilterGio} 를 재사용한다. {@code @Order(16)} — 기존 1~15 와 겹치지 않는 값
 * (securityMatcher 가 배타적이라 순서 의존성은 없다).
 */
@Configuration
public class EfinSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilter;

    public EfinSecurityConfig(JwtAuthenticationFilterGio jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(16)
    public SecurityFilterChain efinSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/efin/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasAnyRole("PARENT", "CHILD"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다."))
                        .accessDeniedHandler((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다.")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}