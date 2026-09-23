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
 * <p>알림은 부모(결제·위치)와 자녀(용돈 받음·유해사이트 등) 양쪽에 저장·발송되므로
 * PARENT/CHILD 모두 조회/읽음이 가능해야 한다. (다른 도메인처럼 {@code securityMatcher}로
 * 알림 경로만 담당한다.)
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
                // 알림 조회/읽음 = 로그인한 부모·자녀
                // [VULN-NOTI-1] recipientId 소유권을 검증하지 않는다(IDOR). — 2026-09-07 실증 확인
                //   recipientId 는 알림 API 의 URL 에 들어가는 '알림 수신자 id'다. 예를 들어
                //   /api/notifications/121 의 121 이 recipientId 이고, "121번 회원이 받은 알림"을
                //   뜻한다. 그런데 아래 규칙은 로그인 여부와 역할(PARENT·CHILD)만 보고, URL 의
                //   그 숫자가 지금 로그인한 본인인지는 어디서도 확인하지 않는다.
                //   → 로그인만 되어 있으면 숫자만 바꿔 남의 알림을 그대로 읽고, 읽음 처리까지 할 수 있다.
                //     읽음 처리가 되면 피해자 화면에서는 새 알림 표시가 사라지므로, 자녀의 유해 사이트
                //     접속이나 결제 알림을 부모가 못 보게 만드는 것도 가능하다. 단순 열람에 그치지 않는다.
                //   방어: 인증 주체의 id 와 recipientId 가 같은지 검사하거나, 애초에 URL 에서
                //         recipientId 를 받지 말고 인증 주체의 id 로 조회한다.
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
