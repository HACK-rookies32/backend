package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 자녀 결제 내역 1건 응답 (No.45). */
@Schema(description = "자녀 결제 내역 1건")
public record QrPaymentHistoryResponse(
        @Schema(description = "QR 토큰") String qrToken,
        @Schema(description = "매장명", example = "GS25 역삼점") String storeName,
        @Schema(description = "결제 금액(원)", example = "3000") long amount,
        @Schema(description = "상태: PAID / CANCELLED", example = "PAID") String status,
        @Schema(description = "결제 시각") LocalDateTime paidAt) {

    public static QrPaymentHistoryResponse from(QrPayment qr) {
        return new QrPaymentHistoryResponse(
                qr.getQrToken(),
                qr.getStoreName(),
                qr.getAmount() != null ? qr.getAmount() : 0L,
                qr.getStatus().name(),
                qr.getPaidAt());
    }
}
