package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** QR 매장결제 취소 응답 (No.46). 취소(환급) 후 자녀 잔액 + PAYMENT_CANCEL 원장 1건. */
@Schema(description = "QR 결제 취소 결과")
public record QrCancelResponse(
        @Schema(description = "QR 토큰") String qrToken,
        @Schema(description = "QR 상태", example = "CANCELLED") String status,
        @Schema(description = "취소(환급) 금액(원)", example = "3000") long canceledAmount,
        @Schema(description = "취소 후 자녀 잔액(원)", example = "5000") long balance,
        @Schema(description = "방금 기록된 취소 원장 1건") TxResponse transaction) {
}
