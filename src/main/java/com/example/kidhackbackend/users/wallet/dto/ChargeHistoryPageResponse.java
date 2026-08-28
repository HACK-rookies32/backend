package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 충전 내역 통합 페이지 (No.37, P-3). */
@Schema(description = "충전 내역 통합 페이지")
public record ChargeHistoryPageResponse(
        @Schema(description = "충전 내역 목록") List<ChargeHistoryResponse> content,
        @Schema(description = "현재 페이지 번호(0부터)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "12") long totalElements,
        @Schema(description = "전체 페이지 수", example = "1") int totalPages) {
}
