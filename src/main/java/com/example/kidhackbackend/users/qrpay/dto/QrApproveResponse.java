package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** QR 스캔 결제 승인 응답 (No.44). 결제 후 자녀 잔액 + PAYMENT 원장 1건. */
@Schema(description = "QR 결제 승인 결과")
public record QrApproveResponse(
        @Schema(description = "QR 토큰") String qrToken,
        @Schema(description = "QR 상태", example = "PAID") String status,
        @Schema(description = "결제 금액(원)", example = "3000") long amount,
        @Schema(description = "매장명", example = "GS25 역삼점") String storeName,
        @Schema(description = "결제 후 자녀 잔액(원)", example = "2000") long balance,
        @Schema(description = "방금 기록된 PAYMENT 원장 1건") TxResponse transaction) {
}
