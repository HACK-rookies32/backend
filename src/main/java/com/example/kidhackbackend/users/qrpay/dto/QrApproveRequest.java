package com.example.kidhackbackend.users.qrpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** QR 스캔 결제 승인 요청 (No.44). 매장 단말이 스캔한 QR과 입력한 금액. 두 필드 모두 필수. */
@Schema(description = "QR 스캔 결제 승인 요청 (매장 단말)")
public record QrApproveRequest(
        @Schema(description = "(필수) 스캔한 결제 QR 토큰", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "qrToken은 필수입니다.") String qrToken,
        @Schema(description = "(필수) 결제 금액(원, 1 이상 — 단말 입력)", example = "3000",
                requiredMode = RequiredMode.REQUIRED)
        @Positive(message = "결제 금액은 1원 이상이어야 합니다.") long amount) {
}
