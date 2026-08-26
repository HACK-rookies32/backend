package com.example.kidhackbackend.users.qrpay.dto;

import java.time.LocalDateTime;

/**
 * 결제 QR 상태 조회 응답 (No.43 폴링). status는 만료 반영이 계산된 유효 상태다.
 */
public record QrStatusResponse(
        String qrToken,
        String status,
        LocalDateTime expiresAt) {
}
