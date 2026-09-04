package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 결제 비밀번호 확인 결과. */
@Schema(description = "결제 비밀번호 확인 결과")
public record PaymentPasswordVerifyResponse(
        @Schema(description = "일치 여부", example = "true") boolean valid) {
}
