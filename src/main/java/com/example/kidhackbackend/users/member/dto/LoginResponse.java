package com.example.kidhackbackend.users.member.dto;

import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "로그인 결과 — 토큰 및 회원 요약")
@Getter
@AllArgsConstructor
public class LoginResponse {

    @Schema(description = "access token (JWT, 유효 1시간)")
    private String token;

    @Schema(description = "refresh token (8자리) — /api/auth/token/refresh 에 사용")
    private String refreshToken;

    @Schema(description = "로그인 아이디", example = "parent01")
    private String loginId;

    @Schema(description = "실명", example = "홍길동")
    private String username;

    @Schema(description = "역할", example = "PARENT")
    private Role role;
}
