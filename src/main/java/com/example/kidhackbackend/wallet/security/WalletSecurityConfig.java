package com.example.kidhackbackend.wallet.security;

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
 * 지갑 도메인 전용 시큐리티 체인 (/api/wallet/**).
 *
 * <p>충전(준비/승인)은 부모(PARENT)만 접근한다. 공통 {@link JwtAuthenticationFilterGio}를 재사용한다.
 * {@code @Order(7)} — location(1)/admin·filtering(2)/device(3)/notification(4)/monitoring(5)/child(6) 다음.
 */
@Configuration
public class WalletSecurityConfig {

    private final JwtAuthenticationFilterGio jwtAuthenticationFilter;

    public WalletSecurityConfig(JwtAuthenticationFilterGio jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(7)
    public SecurityFilterChain walletSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/wallet/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
