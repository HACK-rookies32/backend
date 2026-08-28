package com.example.kidhackbackend.users.qrpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** QR 스캔 결제 승인 요청 (No.44). 매장 단말이 스캔한 QR과 입력한 금액. */
@Schema(description = "QR 스캔 결제 승인 요청 (매장 단말)")
public record QrApproveRequest(
        @Schema(description = "스캔한 결제 QR 토큰")
        @NotBlank(message = "qrToken은 필수입니다.") String qrToken,
        @Schema(description = "결제 금액(원, 1 이상 — 단말 입력)", example = "3000")
        @Positive(message = "결제 금액은 1원 이상이어야 합니다.") long amount) {
}
