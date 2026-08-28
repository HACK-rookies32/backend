package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.Card;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 카드 조회/등록 응답 — 민감값 미포함(마스킹). 규약 확정본: 응답은 암호화하지 않는다.
 */
public record CardResponse(
        @Schema(description = "카드 id", example = "1") Long id,
        @Schema(description = "마스킹된 카드번호(뒤 4자리)", example = "**** **** **** 5678") String maskedCardNumber,
        @Schema(description = "소유자명(실명)", example = "김지영") String ownerName,
        @Schema(description = "유효기간 MM/YY", example = "05/26") String expiry,
        @Schema(description = "등록 시각") LocalDateTime createdAt) {

    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.maskedCardNumber(),
                card.getOwnerName(),
                card.getExpiry(),
                card.getCreatedAt());
    }
}
