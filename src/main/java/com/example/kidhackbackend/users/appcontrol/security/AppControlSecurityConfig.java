package com.example.kidhackbackend.users.appcontrol.security;

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
public class AppControlSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilter;

    public AppControlSecurityConfig(JwtAuthenticationFilterGio jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(13)
    public SecurityFilterChain appControlSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/appcontrol/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 자녀: 설치 앱 목록 동기화
                        .requestMatchers(HttpMethod.POST, "/api/appcontrol/sync").hasRole("CHILD")
                        // 부모: 설치 앱 목록 조회
                        .requestMatchers(HttpMethod.GET, "/api/appcontrol/apps/*").hasRole("PARENT")
                        // 부모: 통제 규칙 추가/수정/삭제
                        .requestMatchers(HttpMethod.POST, "/api/appcontrol/rules").hasRole("PARENT")
                        .requestMatchers(HttpMethod.PATCH, "/api/appcontrol/rules/*").hasRole("PARENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/appcontrol/rules/*").hasRole("PARENT")
                        // 부모/자녀: 통제 규칙 조회 (자녀 앱이 규칙을 받아서 차단 적용)
                        .requestMatchers(HttpMethod.GET, "/api/appcontrol/rules/*").authenticated()
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