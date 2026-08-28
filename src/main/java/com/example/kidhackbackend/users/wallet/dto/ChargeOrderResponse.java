package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 충전 주문 1건 응답 (충전내역 조회 — No.37). */
@Schema(description = "충전 주문 1건")
public record ChargeOrderResponse(
        @Schema(description = "가맹점 주문번호") String merchantUid,
        @Schema(description = "충전 금액(원)", example = "10000") long amount,
        @Schema(description = "주문 상태: READY / PAID / FAILED / REFUNDED / CANCELLED / EXPIRED", example = "PAID") String status,
        @Schema(description = "PG 거래 ID") String pgTransactionId,
        @Schema(description = "생성 시각") LocalDateTime createdAt) {

    public static ChargeOrderResponse from(ChargeOrder order) {
        return new ChargeOrderResponse(
                order.getMerchantUid(),
                order.getAmount(),
                order.getStatus().name(),
                order.getPgTransactionId(),
                order.getCreatedAt());
    }
}
