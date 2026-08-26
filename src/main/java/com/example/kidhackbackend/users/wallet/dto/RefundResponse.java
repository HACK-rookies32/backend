package com.example.kidhackbackend.users.wallet.dto;

/**
 * 환불 응답 (No.37). 환불 후 잔액과 방금 기록된 환불 원장 1건을 담는다.
 */
public record RefundResponse(
        String merchantUid,
        long refundedAmount,
        long balance,
        TxResponse transaction) {
}
