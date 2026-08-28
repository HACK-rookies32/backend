package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Getter;

@Schema(description = "비밀번호 재설정 - 인증코드 발송 요청")
@Getter
public class ResetPasswordSendCodeRequest {

    @Schema(description = "(필수) 비밀번호를 재설정할 계정의 로그인 아이디", example = "child01",
            requiredMode = RequiredMode.REQUIRED)
    private String loginId;

    @Schema(description = "(필수) 본인 이메일 또는 부모의 이메일", example = "parent01@test.com",
            requiredMode = RequiredMode.REQUIRED)
    private String email;
}
