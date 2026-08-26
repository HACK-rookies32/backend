package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import java.time.LocalDateTime;

/**
 * 자녀 결제 내역 1건 응답 (No.45). 매장명·금액·상태·결제시각.
 */
public record QrPaymentHistoryResponse(
        String qrToken,
        String storeName,
        long amount,
        String status,
        LocalDateTime paidAt) {

    public static QrPaymentHistoryResponse from(QrPayment qr) {
        return new QrPaymentHistoryResponse(
                qr.getQrToken(),
                qr.getStoreName(),
                qr.getAmount() != null ? qr.getAmount() : 0L,
                qr.getStatus().name(),
                qr.getPaidAt());
    }
}
