package com.example.kidhackbackend.users.qrpay.entity;

/**
 * 결제 QR(QrPayment) 상태 — QR결제 No.42~46.
 *
 * <p>ISSUED(발급, 결제 대기) → PAID(No.44 승인) 또는 EXPIRED(60초 만료) / CANCELLED(No.46 취소).
 * No.42(발급)는 ISSUED만 생성하고, 나머지 전이는 폴링(No.43)·승인(No.44)·취소(No.46)에서 다룬다.
 */
public enum QrStatus {
    ISSUED,
    PAID,
    EXPIRED,
    CANCELLED
}
