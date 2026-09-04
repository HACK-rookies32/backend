package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;

/** 결제 비밀번호 사전 확인 요청(거래 전 UI 점검용 — 최종 검증은 거래 관문이 한다). */
@Schema(description = "결제 비밀번호 확인 요청")
public record PaymentPasswordVerifyRequest(
        @Schema(description = "(필수) 결제 비밀번호", example = "1357", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "결제 비밀번호는 필수입니다.") String password) {
}
