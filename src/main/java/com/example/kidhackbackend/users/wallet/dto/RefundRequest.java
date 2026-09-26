package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 충전 환불 요청 본문 — 환불받을 카드 지정(선택).
 *
 * <p>{@code cardId} 가 있으면 그 카드로 mock PG 환불(payout)을 태우고, 없으면(null) 기존처럼
 * 지갑 잔액만 차감한다.
 *
 * <p>⚠️ [VULN-WALLET-7] 서비스가 이 {@code cardId} 의 소유자를 검증하지 않는다 —
 * 남의 계정 잔액을 <b>공격자 카드로</b> 환불 수취할 수 있다(환불 목적지 IDOR).
 */
@Schema(description = "충전 환불 요청 (환불받을 카드 지정)")
public record RefundRequest(
        @Schema(description = "환불받을 카드 id (선택). 없으면 지갑 잔액만 차감", example = "3")
        Long cardId) {
}
