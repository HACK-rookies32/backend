package com.example.kidhackbackend.users.filtering.security;

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
 * 유해사이트/앱 차단 도메인 전용 시큐리티 체인 (/api/filtering/**).
 *
 * <p>인가 정책:
 * <ul>
 *   <li>POST /visit          → CHILD (자녀 앱이 접속 대상 보고)</li>
 *   <li>차단규칙 등록          → PARENT</li>
 *   <li>관리자 유해키워드 조회  → PARENT (전역 정책 읽기 전용, #2)</li>
 *   <li>차단규칙 조회(GET)     → PARENT + CHILD. 자녀 앱의 접근성 서비스가 <b>기기 안에서만</b>
 *       규칙을 매칭하려고 읽는다(자녀 UI에는 노출되지 않는다). CHILD 는 컨트롤러에서
 *       본인 childId 만 허용한다.</li>
 *   <li>차단규칙 삭제(DELETE) → 인증만 요구 ([VULN-FILT-1]: 권한/소유권 제한 없음)</li>
 * </ul>
 */
@Configuration
public class FilteringSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilter;

    public FilteringSecurityConfig(JwtAuthenticationFilterGio jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filteringSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/filtering/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 자녀: 접속 대상 보고
                        .requestMatchers(HttpMethod.POST, "/api/filtering/visit").hasRole("CHILD")
                        // 부모: 차단규칙 등록
                        .requestMatchers(HttpMethod.POST, "/api/filtering/rules").hasRole("PARENT")
                        // 부모: 관리자 지정 전역 유해 키워드 읽기 전용 조회(#2). 전역 정책 데이터라 childId 없음.
                        .requestMatchers(HttpMethod.GET, "/api/filtering/harmful-keywords").hasRole("PARENT")
                        // 부모 + 자녀 앱: 차단규칙 조회. 자녀 기기가 로컬 매칭에 쓴다(본인 childId 만, 컨트롤러에서 검사).
                        .requestMatchers(HttpMethod.GET, "/api/filtering/rules/*").hasAnyRole("PARENT", "CHILD")
                        // [VULN-FILT-1] 삭제는 권한/소유권 제한 없이 '인증만' 되면 허용
                        //   → 다른 부모는 물론, 감시당하는 자녀(CHILD)도 차단을 스스로 해제 가능
                        .requestMatchers(HttpMethod.DELETE, "/api/filtering/rules/*").authenticated()
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
