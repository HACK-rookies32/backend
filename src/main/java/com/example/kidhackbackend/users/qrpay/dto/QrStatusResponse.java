package com.example.kidhackbackend.users.qrpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 결제 QR 상태 조회 응답 (No.43 폴링). status 는 만료 반영이 계산된 유효 상태. */
@Schema(description = "결제 QR 상태")
public record QrStatusResponse(
        @Schema(description = "QR 토큰") String qrToken,
        @Schema(description = "상태: ISSUED / PAID / EXPIRED / CANCELLED", example = "ISSUED") String status,
        @Schema(description = "만료 시각") LocalDateTime expiresAt) {
}
