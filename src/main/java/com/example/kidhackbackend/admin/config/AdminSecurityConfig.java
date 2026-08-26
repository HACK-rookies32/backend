package com.example.kidhackbackend.admin.config;

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

@Configuration
@RequiredArgsConstructor
public class AdminSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilterGio;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    @Order(2)
    public SecurityFilterChain adminMemberFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/admin/members/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("SUPER_ADMIN"))
                .addFilterBefore(jwtAuthenticationFilterGio, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler));
        return http.build();
    }
    // 커뮤니티 관리자 경로 필터 체인 추가
    @Bean
    @Order(3)
    public SecurityFilterChain adminCommunityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/admin/community/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 커뮤니티 관리 기능(조회/삭제/사용자 밴)은 SUPER_ADMIN, COMMUNITY_ADMIN 만 가능
                        .anyRequest().hasAnyRole("SUPER_ADMIN", "COMMUNITY_ADMIN")
                )
                .addFilterBefore(jwtAuthenticationFilterGio, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler));
        return http.build();
    }

    // 공지사항 super admin만
    @Bean
    @Order(4)
    public SecurityFilterChain adminNoticeFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/admin/notices/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("SUPER_ADMIN")
                )
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(jwtAuthenticationFilterGio, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // 전체 관리자 모두 가능
    @Bean
    @Order(10)
    public SecurityFilterChain adminNotificationBroadcastFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/admin/notifications/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasAnyRole("SUPER_ADMIN", "GENERAL_ADMIN", "COMMUNITY_ADMIN", "INQUIRY_ADMIN")
                )
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(jwtAuthenticationFilterGio, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}