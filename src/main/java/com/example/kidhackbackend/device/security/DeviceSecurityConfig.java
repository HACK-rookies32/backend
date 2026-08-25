package com.example.kidhackbackend.device.security;

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

/**
 * 기기 감지 도메인 전용 시큐리티 체인 (/api/device/**).
 *
 * <p>전원/GPS 이벤트 보고는 자녀 앱이 수행하므로 CHILD 권한을 요구한다.
 */
@Configuration
public class DeviceSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilter;

    public DeviceSecurityConfig(JwtAuthenticationFilterGio jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(3)
    public SecurityFilterChain deviceSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/device/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/device/event").hasRole("CHILD")
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
