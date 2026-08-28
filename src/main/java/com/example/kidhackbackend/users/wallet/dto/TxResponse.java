package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 지갑 원장 1건 응답. */
@Schema(description = "지갑 원장 1건")
public record TxResponse(
        @Schema(description = "원장 ID", example = "1") Long id,
        @Schema(description = "거래 유형: CHARGE / REFUND / TRANSFER_IN / TRANSFER_OUT / PAYMENT 등", example = "CHARGE") String type,
        @Schema(description = "거래 금액(원)", example = "10000") long amount,
        @Schema(description = "거래 후 잔액(원)", example = "15000") long balanceAfter,
        @Schema(description = "참조 UID(merchantUid/qrToken 등)") String refUid,
        @Schema(description = "생성 시각") LocalDateTime createdAt) {

    public static TxResponse from(WalletTransaction tx) {
        return new TxResponse(
                tx.getId(),
                tx.getType().name(),
                tx.getAmount(),
                tx.getBalanceAfter(),
                tx.getRefUid(),
                tx.getCreatedAt());
    }
}
