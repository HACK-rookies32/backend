package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 결제 QR 발급 응답 (No.42). 1회성 토큰 + 상태 + 만료시각(60초). */
@Schema(description = "결제 QR 발급 결과")
public record QrIssueResponse(
        @Schema(description = "1회성 QR 토큰(매장 단말이 스캔)") String qrToken,
        @Schema(description = "QR 상태", example = "ISSUED") String status,
        @Schema(description = "만료 시각(발급 + 60초)") LocalDateTime expiresAt) {

    public static QrIssueResponse from(QrPayment qr) {
        return new QrIssueResponse(qr.getQrToken(), qr.getStatus().name(), qr.getExpiresAt());
    }
}
