package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 환불 응답 (No.37). 환불 후 잔액과 방금 기록된 환불 원장 1건. */
@Schema(description = "충전 환불 결과")
public record RefundResponse(
        @Schema(description = "가맹점 주문번호") String merchantUid,
        @Schema(description = "환불 금액(원)", example = "10000") long refundedAmount,
        @Schema(description = "환불 후 잔액(원)", example = "5000") long balance,
        @Schema(description = "환불이 나간 카드(마스킹). 카드 지정 환불이 아니면 null") String maskedCard,
        @Schema(description = "PG 환불 거래 ID(mock). 카드 지정 환불이 아니면 null") String pgRefundId,
        @Schema(description = "방금 기록된 환불 원장 1건") TxResponse transaction) {
}
