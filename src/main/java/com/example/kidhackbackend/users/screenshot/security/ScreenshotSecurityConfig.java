package com.example.kidhackbackend.users.screenshot.security;

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
 * 화면 캡처 도메인 전용 시큐리티 체인 (/api/children/&#42;/screenshots/**).
 *
 * <p>업로드는 자녀 앱(CHILD), 목록 조회와 추가 촬영 요청은 부모(PARENT)가 수행한다.
 * 한 경로 아래에서 역할이 갈리므로 MonitoringSecurityConfig 와 같은 모양이다.
 *
 * <p><b>⚠️ @Order 는 반드시 ChildSecurityConfig(6) 보다 낮아야 한다.</b>
 * 그쪽은 {@code /api/children/**} 전체를 잡아 {@code anyRequest().hasRole("PARENT")}
 * 를 걸기 때문에, 이 체인이 뒤로 밀리면 캡처 경로까지 그 규칙에 먹힌다.
 * 그러면 자녀 앱의 업로드가 403 이 되어 명세(4.6 업로드 = CHILD)와 어긋난다.
 * 체인은 등록 순서대로 첫 매칭에서 멈추므로, 더 좁은 이 체인이 먼저 와야 한다.
 */
@Configuration
public class ScreenshotSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilter;

    public ScreenshotSecurityConfig(JwtAuthenticationFilterGio jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(5)
    public SecurityFilterChain screenshotSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // 목록/업로드는 끝이 screenshots 라 첫 패턴이, /request 는 둘째 패턴이 잡는다.
                .securityMatcher("/api/children/*/screenshots", "/api/children/*/screenshots/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/children/*/screenshots/request").hasRole("PARENT")
                        .requestMatchers(HttpMethod.POST, "/api/children/*/screenshots").hasRole("CHILD")
                        .requestMatchers(HttpMethod.GET, "/api/children/*/screenshots").hasRole("PARENT")
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
