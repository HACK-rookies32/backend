package com.example.kidhackbackend.users.notice.security;

import com.example.kidhackbackend.global.jwt.JwtAuthenticationFilterGio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class NoticeSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilterGio;

    public NoticeSecurityConfig(JwtAuthenticationFilterGio jwtAuthenticationFilterGio) {
        this.jwtAuthenticationFilterGio = jwtAuthenticationFilterGio;
    }

    @Bean
    @Order(9)
    public SecurityFilterChain noticeFilterChain(HttpSecurity http) throws Exception {
        // 공지 열람은 인증 불필요 — 부모/자녀 누구나(비로그인 포함) 볼 수 있다.
        // (JWT 필터는 그대로 태워서, 토큰이 있으면 principal 은 채워진다.)
        http
                .securityMatcher("/api/notices/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(jwtAuthenticationFilterGio, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}