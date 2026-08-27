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
        int totalPages) {

    public static AdminTxPageResponse from(Page<WalletTransaction> page) {
        return new AdminTxPageResponse(
                page.getContent().stream().map(TxResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
