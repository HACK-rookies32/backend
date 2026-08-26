package com.example.kidhackbackend.wallet.dto;

import com.example.kidhackbackend.wallet.entity.WalletTransaction;
import java.time.LocalDateTime;

/**
 * 지갑 원장 1건 응답.
 */
public record TxResponse(
        Long id,
        String type,
        long amount,
        long balanceAfter,
        String refUid,
        LocalDateTime createdAt) {

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
