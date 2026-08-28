package com.example.kidhackbackend.users.wallet.entity;

/**
 * 충전 주문(ChargeOrder) 상태.
 *
 * <ul>
 *   <li>{@code READY}     — 준비 완료, PG 결제 대기</li>
 *   <li>{@code PAID}      — 승인 완료, 잔액 적립됨</li>
 *   <li>{@code FAILED}    — PG 가 결제 실패/취소 웹훅을 보냄 (No.36)</li>
 *   <li>{@code REFUNDED}  — 결제됐다가 환불됨 (No.37)</li>
 *   <li>{@code CANCELLED} — 사용자가 결제 진행 중 취소 (READY 에서만 전이)</li>
 *   <li>{@code EXPIRED}   — approve 없이 방치돼 자동 만료된 READY 주문</li>
 * </ul>
 */
public enum ChargeStatus {
    READY,
    PAID,
    FAILED,
    REFUNDED,
    CANCELLED,
    EXPIRED
}
