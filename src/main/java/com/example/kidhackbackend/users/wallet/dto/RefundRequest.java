package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 충전 환불 요청. body 는 <b>선택</b> — 없거나 {@code cardId} 가 null 이면 기존처럼 지갑 잔액 환불.
 *
 * <p>{@code cardId} 를 주면 그 카드로 mock PG 환불(payout)을 태운다.
 * ⚠️ <b>[VULN-WALLET-7]</b> 서버는 이 {@code cardId} 가 요청자(또는 주문)의 것인지 검증하지 않는다
 * (see {@code WalletRefundService.refund}). mock PG 전용 — 실결제망 미연동.
 */
@Schema(description = "충전 환불 요청 (선택 body)")
public record RefundRequest(
        @Schema(description = "(선택) 환불 payout 을 태울 카드 id. 주면 그 카드로 mock PG 환불.", example = "7")
        Long cardId) {
}
