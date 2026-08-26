package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 내 지갑 조회 응답 (No.38). 잔액 + type/기간 필터·페이징된 원장.
 */
public record MyWalletResponse(
        long balance,
        List<TxResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static MyWalletResponse from(long balance, Page<WalletTransaction> page) {
        return new MyWalletResponse(
                balance,
                page.getContent().stream().map(TxResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /** 지갑이 없을 때(거래 0건) 빈 응답. */
    public static MyWalletResponse empty(int page, int size) {
        return new MyWalletResponse(0L, List.of(), page, size, 0L, 0);
    }
}
