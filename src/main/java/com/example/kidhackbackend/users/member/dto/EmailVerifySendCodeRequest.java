package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Getter;

/**
 * 회원가입 이메일 인증 - 인증코드 발송 요청.
 *
 * <p>가입 폼에서 이메일을 입력한 뒤 「인증코드 받기」를 누를 때 부른다. 로그인 전 단계라
 * 인증(토큰) 없이 호출한다({@code /api/auth/**} 는 permitAll).
 */
@Schema(description = "회원가입 이메일 인증 - 인증코드 발송 요청")
@Getter
public class EmailVerifySendCodeRequest {

    @Schema(description = "(필수) 인증코드를 받을 가입 이메일", example = "newuser@test.com",
            requiredMode = RequiredMode.REQUIRED)
    private String email;
}
