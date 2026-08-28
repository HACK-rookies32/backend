package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** 지갑 원장 페이지 (No.37). */
@Schema(description = "지갑 원장 페이지")
public record TxPageResponse(
        @Schema(description = "원장 목록") List<TxResponse> content,
        @Schema(description = "현재 페이지 번호(0부터)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "42") long totalElements,
        @Schema(description = "전체 페이지 수", example = "3") int totalPages) {

    public static TxPageResponse from(Page<WalletTransaction> page) {
        return new TxPageResponse(
                page.getContent().stream().map(TxResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /** 지갑이 없을 때(원장 0건) 빈 응답. */
    public static TxPageResponse empty(int page, int size) {
        return new TxPageResponse(List.of(), page, size, 0L, 0);
    }
}
