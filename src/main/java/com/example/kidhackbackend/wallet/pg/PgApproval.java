package com.example.kidhackbackend.wallet.pg;

/**
 * PG 승인 결과(모의).
 *
 * @param pgTransactionId PG 거래 ID (mock: imp_...)
 * @param paidAmount      PG가 '실제로 결제했다고 보고'하는 금액(원)
 * @param status          승인 상태 문자열 (mock: "paid")
 */
public record PgApproval(String pgTransactionId, long paidAmount, String status) {
}
