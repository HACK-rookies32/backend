package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * PG 결제 웹훅(콜백) 본문 — 역할표 No.36. 서명(X-PG-Signature)으로 진위를 검증한다.
 * PG 서버가 보내는 값이라 4개 필드 모두 채워져 온다.
 */
@Schema(description = "PG 결제 웹훅 페이로드")
public record PgWebhookRequest(
        @Schema(description = "(필수) 가맹점 주문번호(준비 단계 발급)", example = "kid_ab12cd34",
                requiredMode = RequiredMode.REQUIRED) String merchantUid,
        @Schema(description = "(필수) 결제 상태. \"paid\" 면 확정, 그 외는 확정하지 않음", example = "paid",
                requiredMode = RequiredMode.REQUIRED) String status,
        @Schema(description = "(필수) PG 가 통지한 결제 금액(원)", example = "10000",
                requiredMode = RequiredMode.REQUIRED) long paidAmount,
        @Schema(description = "(필수) PG 거래 ID", example = "imp_9f8e7d6c",
                requiredMode = RequiredMode.REQUIRED) String pgTransactionId) {
}
