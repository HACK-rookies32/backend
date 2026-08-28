package com.example.kidhackbackend.admin.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 결제 통계 대시보드 응답 (No.51). groupBy(DAY/MERCHANT)별 집계 행 목록. */
@Schema(description = "결제 통계 대시보드")
public record FinanceStatsResponse(
        @Schema(description = "집계 기준: DAY / MERCHANT", example = "DAY") String groupBy,
        @Schema(description = "집계 행 목록") List<StatRow> rows) {

    @Schema(description = "집계 1행 (DAY: key=일자 / MERCHANT: key=매장명)")
    public record StatRow(
            @Schema(description = "집계 키(일자 또는 매장명)", example = "2026-08-27") String key,
            @Schema(description = "충전 합계(원). MERCHANT 집계 시 0", example = "50000") long chargeSum,
            @Schema(description = "결제 합계(원)", example = "12000") long paymentSum,
            @Schema(description = "건수", example = "8") long count) {
    }
}
