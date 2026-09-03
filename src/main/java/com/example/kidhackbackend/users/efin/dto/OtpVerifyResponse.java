package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 거래 OTP 확인 결과. */
@Schema(description = "거래 OTP 확인 결과")
public record OtpVerifyResponse(
        @Schema(description = "유효 여부", example = "true") boolean valid) {
}