package com.example.kidhackbackend.users.wallet.dto;

/**
 * 복호된 카드 평문(서버 내부 전용 — 응답으로 절대 노출하지 않는다).
 *
 * <p>규약 확정본의 payload 스키마와 1:1. {@code cvc}는 복호는 되지만 <b>저장하지 않는다</b>.
 * {@code nonce}·{@code timestamp}는 재전송 방지 검증에만 쓰고 저장하지 않는다.
 *
 * <p>{@code paymentPassword}(4자리)는 접근매체 발급 시 본인확인용으로 함께 실려 온다 — 확인만 하고 저장하지 않는다.
 */
public record CardPlain(
        String cardNumber,
        String expiry,
        String cvc,
        String passwordFront,
        String birthDate,
        String ownerName,
        String paymentPassword,
        String nonce,
        Long timestamp) {
}
