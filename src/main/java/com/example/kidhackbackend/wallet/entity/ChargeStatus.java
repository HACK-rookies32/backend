package com.example.kidhackbackend.wallet.entity;

/**
 * 충전 주문(ChargeOrder) 상태.
 *
 * <p>READY(준비 완료, PG 결제 대기) → PAID(승인 완료, 잔액 적립) 로 전이한다.
 * CANCELLED는 이후 취소/환불(No.37) 확장용으로 자리만 확보.
 */
public enum ChargeStatus {
    READY,
    PAID,
    CANCELLED
}
