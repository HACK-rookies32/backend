package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이디 찾기 - 코드 검증 결과")
public record FindIdVerifyResponse(
        @Schema(description = "조회된 로그인 아이디", example = "child01") String loginId
) {
}
