package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 관리자 전체 거래 내역 페이징 응답 (No.47).
 */
public record AdminTxPageResponse(
        List<TxResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        Long walletBalance) {   // 추가: userId로 조회했을 때만 값 채워짐, 전체 조회 시 null

    public static AdminTxPageResponse from(Page<WalletTransaction> page) {
        return from(page, null);
    }

    public static AdminTxPageResponse from(Page<WalletTransaction> page, Long walletBalance) {
        return new AdminTxPageResponse(
                page.getContent().stream().map(TxResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                walletBalance);
    }
}
