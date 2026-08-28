package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 자녀 지갑 조회(부모) 응답 (No.41). 자녀 잔액 + 최근거래 요약. */
@Schema(description = "자녀 지갑 (잔액 + 최근 거래)")
public record ChildWalletResponse(
        @Schema(description = "자녀 회원 ID", example = "7") Long childId,
        @Schema(description = "잔액(원)", example = "5000") long balance,
        @Schema(description = "최근 거래 요약(최근 5건)") List<TxResponse> recentTransactions) {

    public static ChildWalletResponse of(Long childId, long balance, List<WalletTransaction> recent) {
        return new ChildWalletResponse(
                childId,
                balance,
                recent.stream().map(TxResponse::from).toList());
    }
}
