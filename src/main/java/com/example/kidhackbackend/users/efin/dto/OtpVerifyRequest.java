package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;

/** 거래 OTP 사전 확인 요청(송금 전 UI 단계에서 코드 유효성만 점검). */
@Schema(description = "거래 OTP 확인 요청")
public record OtpVerifyRequest(
        @Schema(description = "(필수) 입력한 인증번호", example = "428193", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "인증번호는 필수입니다.") String code) {
}