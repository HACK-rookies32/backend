package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 충전 내역 통합 항목 (No.37, P-3).
 *
 * <p>충전 주문 정보에 환불 시각을 합쳐 한 줄로 보여준다. 프론트가 주문/원장을 따로 받아 조합할 필요가 없다.
 */
@Schema(description = "충전 내역 통합 항목")
public record ChargeHistoryResponse(
        @Schema(description = "가맹점 주문번호", example = "kid_ab12cd34") String merchantUid,
        @Schema(description = "충전 금액(원)", example = "10000") long amount,
        @Schema(description = "상태: READY / PAID / FAILED / REFUNDED / CANCELLED / EXPIRED", example = "PAID") String status,
        @Schema(description = "PG 거래 ID (승인 전이면 null)") String pgTransactionId,
        @Schema(description = "주문 생성 시각") LocalDateTime createdAt,
        @Schema(description = "환불 시각 (환불된 경우만, 아니면 null)", nullable = true) LocalDateTime refundedAt) {
}
