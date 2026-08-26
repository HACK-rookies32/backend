package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import java.time.LocalDateTime;

/**
 * 결제 QR 발급 응답 (No.42). 발급된 1회성 토큰과 상태·만료시각을 담는다.
 */
public record QrIssueResponse(
        String qrToken,
        String status,
        LocalDateTime expiresAt) {

    public static QrIssueResponse from(QrPayment qr) {
        return new QrIssueResponse(qr.getQrToken(), qr.getStatus().name(), qr.getExpiresAt());
    }
}
