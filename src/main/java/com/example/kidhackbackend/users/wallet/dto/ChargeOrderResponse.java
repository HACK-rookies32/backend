package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import java.time.LocalDateTime;

/**
 * 충전 주문 1건 응답 (충전내역 조회 — No.37).
 */
public record ChargeOrderResponse(
        String merchantUid,
        long amount,
        String status,
        String pgTransactionId,
        LocalDateTime createdAt) {

    public static ChargeOrderResponse from(ChargeOrder order) {
        return new ChargeOrderResponse(
                order.getMerchantUid(),
                order.getAmount(),
                order.getStatus().name(),
                order.getPgTransactionId(),
                order.getCreatedAt());
    }
}
