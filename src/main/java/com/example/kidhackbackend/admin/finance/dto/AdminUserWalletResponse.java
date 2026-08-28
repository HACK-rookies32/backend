package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 관리자용 특정 회원 지갑 조회 응답 (No.49). */
@Schema(description = "관리자 - 회원 지갑 (잔액 + 최근 거래)")
public record AdminUserWalletResponse(
        @Schema(description = "회원 ID", example = "7") Long userId,
        @Schema(description = "잔액(원)", example = "15000") long balance,
        @Schema(description = "최근 거래 요약") List<TxResponse> recentTransactions) {

    public static AdminUserWalletResponse of(Long userId, long balance, List<WalletTransaction> recent) {
        return new AdminUserWalletResponse(
                userId,
                balance,
                recent.stream().map(TxResponse::from).toList());
    }
}
