package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.wallet.dto.TxResponse;

/**
 * QR 스캔 결제 승인 응답 (No.44). 결제 후 자녀 잔액과 방금 기록된 PAYMENT 원장 1건을 담는다.
 */
public record QrApproveResponse(
        String qrToken,
        String status,
        long amount,
        String storeName,
        long balance,
        TxResponse transaction) {
}
