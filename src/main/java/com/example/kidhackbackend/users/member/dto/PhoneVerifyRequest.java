package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Getter;

/** 회원가입 휴대폰 인증 - 코드 검증 요청. */
@Schema(description = "회원가입 휴대폰 인증 - 코드 검증 요청")
@Getter
public class PhoneVerifyRequest {

    @Schema(description = "(필수) 인증코드를 받은 휴대폰 번호", example = "010-1111-2222",
            requiredMode = RequiredMode.REQUIRED)
    private String phoneNumber;

    @Schema(description = "(필수) 발송받은 6자리 인증코드", example = "126207",
            requiredMode = RequiredMode.REQUIRED)
    private String code;
}
