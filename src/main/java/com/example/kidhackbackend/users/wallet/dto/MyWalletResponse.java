package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** 내 지갑 조회 응답 (No.38). 잔액 + type/기간 필터·페이징된 원장. */
@Schema(description = "내 지갑 (잔액 + 페이징 원장)")
public record MyWalletResponse(
        @Schema(description = "잔액(원)", example = "15000") long balance,
        @Schema(description = "거래 원장 목록") List<TxResponse> content,
        @Schema(description = "현재 페이지 번호(0부터)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "37") long totalElements,
        @Schema(description = "전체 페이지 수", example = "2") int totalPages) {

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
