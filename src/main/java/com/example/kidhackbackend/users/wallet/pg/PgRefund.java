package com.example.kidhackbackend.users.wallet.pg;

/**
 * PG 카드 환불(payout) 결과(모의).
 *
 * <p>충전 승인({@link PgApproval})과 대칭인 환불 결과다. 실제 카드사 환불망 대신
 * {@link MockPgClient} 가 항상 성공을 반환한다.
 *
 * @param pgRefundId     PG 환불 거래 ID (mock: imprf_...)
 * @param refundedAmount 환불된 금액(원)
 * @param maskedCard     환불이 나간 카드 마스킹번호(응답/감사용)
 * @param status         환불 상태 문자열 (mock: "refunded")
 */
public record PgRefund(String pgRefundId, long refundedAmount, String maskedCard, String status) {
}
