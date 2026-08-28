package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * PG 결제 웹훅(콜백) 본문 — 역할표 No.36. 서명(X-PG-Signature)으로 진위를 검증한다.
 */
@Schema(description = "PG 결제 웹훅 페이로드")
public record PgWebhookRequest(
        @Schema(description = "가맹점 주문번호(준비 단계 발급)") String merchantUid,
        @Schema(description = "결제 상태. \"paid\" 면 확정, 그 외는 확정하지 않음", example = "paid") String status,
        @Schema(description = "PG 가 통지한 결제 금액(원)", example = "10000") long paidAmount,
        @Schema(description = "PG 거래 ID") String pgTransactionId) {
}
