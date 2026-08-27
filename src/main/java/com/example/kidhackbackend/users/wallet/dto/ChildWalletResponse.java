package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import java.util.List;

/**
 * 자녀 지갑 조회(부모) 응답 (No.41). 자녀 잔액 + 최근거래 요약(최근 5건).
 */
public record ChildWalletResponse(
        Long childId,
        long balance,
        List<TxResponse> recentTransactions) {

    public static ChildWalletResponse of(Long childId, long balance, List<WalletTransaction> recent) {
        return new ChildWalletResponse(
                childId,
                balance,
                recent.stream().map(TxResponse::from).toList());
    }
}
