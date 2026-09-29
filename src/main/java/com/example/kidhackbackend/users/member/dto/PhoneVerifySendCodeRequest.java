package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Getter;

/**
 * 회원가입 휴대폰 인증 - 인증코드 발송 요청.
 *
 * <p>아이디 찾기({@link FindIdSendCodeRequest})와 동형이나, 계정 조회 없이 전화번호만 받아
 * 6자리 코드를 "발송한 척"(mock) 하고 응답에 코드를 돌려준다. 로그인 불필요.
 */
@Schema(description = "회원가입 휴대폰 인증 - 인증코드 발송 요청")
@Getter
public class PhoneVerifySendCodeRequest {

    @Schema(description = "(필수) 인증코드를 받을 휴대폰 번호", example = "010-1111-2222",
            requiredMode = RequiredMode.REQUIRED)
    private String phoneNumber;
}
