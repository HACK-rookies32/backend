package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Getter;

@Schema(description = "토큰 갱신 요청")
@Getter
public class TokenRefreshRequest {

    @Schema(description = "(필수) 로그인 시 발급받은 refresh token", example = "aZ3kP9xQ",
            requiredMode = RequiredMode.REQUIRED)
    private String refreshToken;
}
