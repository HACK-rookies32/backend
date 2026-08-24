package com.example.kidhackbackend.location.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 위치 도메인 전용 시큐리티 체인.
 *
 * <p>{@code securityMatcher}로 <b>/api/location/** 경로만</b> 담당하며, {@link Order}가 낮아
 * 팀 공통 {@code SecurityConfig}(permitAll)보다 먼저 매칭된다. 따라서 다른 도메인에는 영향이 없다.
 *
 * <p>인가 정책(역할 기반):
 * <ul>
 *   <li>POST /api/location/track  → CHILD (자녀 기기가 자기 위치 업로드)</li>
 *   <li>그 외 조회/등록           → PARENT (부모가 공간 등록·동선/이벤트 조회)</li>
 * </ul>
 */
@Configuration
public class LocationSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public LocationSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain locationSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/location/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 자녀: 위치 업로드
                        .requestMatchers(HttpMethod.POST, "/api/location/track").hasRole("CHILD")
                        // 부모: 동선/공간/이벤트 조회 및 공간 등록
                        .requestMatchers(HttpMethod.GET, "/api/location/track/*").hasRole("PARENT")
                        .requestMatchers("/api/location/places/**").hasRole("PARENT")
                        .requestMatchers("/api/location/events/**").hasRole("PARENT")
                        // 그 외 위치 경로는 인증만 되면 허용
                        .anyRequest().authenticated())
                // 토큰 없음/유효하지 않음 → 401, 권한 부족 → 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다."))
                        .accessDeniedHandler((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다.")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
