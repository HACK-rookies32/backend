package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Getter;

@Schema(description = "아이디 찾기 - 코드 검증 요청")
@Getter
public class FindIdVerifyRequest {

    @Schema(description = "(필수) 인증코드를 받은 이메일", example = "parent01@test.com",
            requiredMode = RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "(필수) 발송받은 인증코드", example = "126207", requiredMode = RequiredMode.REQUIRED)
    private String code;
}
