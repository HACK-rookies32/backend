package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.wallet.dto.TxResponse;

/**
 * QR 매장결제 취소 응답 (No.46). 취소(환급) 후 자녀 잔액과 방금 기록된 PAYMENT_CANCEL 원장 1건을 담는다.
 */
public record QrCancelResponse(
        String qrToken,
        String status,
        long canceledAmount,
        long balance,
        TxResponse transaction) {
}
