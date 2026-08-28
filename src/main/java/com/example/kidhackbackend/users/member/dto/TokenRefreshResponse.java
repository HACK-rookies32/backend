package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 갱신 결과")
public record TokenRefreshResponse(
        @Schema(description = "새로 발급된 access token") String token
) {
}
