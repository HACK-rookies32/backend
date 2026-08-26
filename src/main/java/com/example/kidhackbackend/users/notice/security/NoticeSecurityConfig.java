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
        http
                .securityMatcher("/api/notices/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(jwtAuthenticationFilterGio, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}