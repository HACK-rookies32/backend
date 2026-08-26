package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import java.util.List;

/**
 * 관리자용 특정 회원 지갑 조회 응답 (No.49). 잔액 + 최근거래 요약.
 */
public record AdminUserWalletResponse(
        Long userId,
        long balance,
        List<TxResponse> recentTransactions) {

    public static AdminUserWalletResponse of(Long userId, long balance, List<WalletTransaction> recent) {
        return new AdminUserWalletResponse(
                userId,
                balance,
                recent.stream().map(TxResponse::from).toList());
    }
}
