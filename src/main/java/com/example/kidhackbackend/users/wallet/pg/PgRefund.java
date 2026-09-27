package com.example.kidhackbackend.users.wallet.pg;

/**
 * PG 환불(payout) 결과(모의). 충전 환불에서 {@code cardId} 를 지정했을 때 발급된다.
 *
 * @param pgRefundId     PG 환불 거래 ID (mock: imprf_...)
 * @param refundedAmount PG가 '환불했다고 보고'하는 금액(원)
 * @param maskedCard     환불이 나간 카드(마스킹, 뒤 4자리)
 * @param status         환불 상태 문자열 (mock: "refunded")
 */
public record PgRefund(String pgRefundId, long refundedAmount, String maskedCard, String status) {
}
