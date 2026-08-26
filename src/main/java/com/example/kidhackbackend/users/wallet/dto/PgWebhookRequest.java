package com.example.kidhackbackend.users.wallet.dto;

/**
 * PG 결제 웹훅(콜백) 본문 — 역할표 No.36.
 *
 * <p>PG 서버가 결제 결과를 서버로 통지할 때 보내는 페이로드. 서명(X-PG-Signature)으로 진위를 검증한다.
 *
 * @param merchantUid     가맹점 주문번호(준비 단계 발급)
 * @param status          결제 상태("paid" 면 확정, 그 외는 확정하지 않음)
 * @param paidAmount      PG가 통지한 결제 금액(원)
 * @param pgTransactionId PG 거래 ID
 */
public record PgWebhookRequest(
        String merchantUid,
        String status,
        long paidAmount,
        String pgTransactionId) {
}
