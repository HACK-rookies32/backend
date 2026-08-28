package com.example.kidhackbackend.users.pairing.security;

import com.example.kidhackbackend.global.jwt.JwtAuthenticationFilterGio;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class PairingSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilter;

    public PairingSecurityConfig(JwtAuthenticationFilterGio jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // 주의: @Order(7)은 임시값. 다른 도메인 SecurityConfig(ChildSecurityConfig가 6번 사용 중)랑
    // 겹치는 번호 없는지 확인하고 필요하면 바꿔줘.
    @Bean
    @Order(7)
    public SecurityFilterChain pairingSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/pairing/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/pairing/code").hasRole("PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/pairing/code").hasRole("PARENT")
                        .requestMatchers(HttpMethod.POST, "/api/pairing/connect").hasRole("CHILD")
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