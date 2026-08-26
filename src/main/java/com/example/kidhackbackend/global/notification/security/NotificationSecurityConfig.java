package com.example.kidhackbackend.global.notification.security;

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
 * 알림 도메인 전용 시큐리티 체인 (/api/notifications/**).
 *
 * <p>알림 조회/읽음은 부모가 확인하는 기능이므로 PARENT 권한을 요구한다.
 * (다른 도메인처럼 {@code securityMatcher}로 알림 경로만 담당한다.)
 */
@Configuration
public class NotificationSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilter;

    public NotificationSecurityConfig(JwtAuthenticationFilterGio jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(11)
    public SecurityFilterChain notificationSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/notifications/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 알림 조회/읽음 = 부모만
                // [VULN-NOTI-1] childId(=수신자) 소유권은 검증하지 않는다.
                //   → 부모 권한만 있으면 임의 recipientId로 남의 알림을 조회/읽음 처리할 수 있다(IDOR).
                //     (수신자를 부모 Member로 매핑하는 2단계에서 해결 예정)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("PARENT"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다."))
                        .accessDeniedHandler((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다.")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
