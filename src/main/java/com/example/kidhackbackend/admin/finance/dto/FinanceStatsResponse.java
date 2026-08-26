package com.example.kidhackbackend.admin.finance.dto;

import java.util.List;

/**
 * 결제 통계 대시보드 응답 (No.51). groupBy(DAY/MERCHANT)별 집계 행 목록.
 */
public record FinanceStatsResponse(
        String groupBy,
        List<StatRow> rows) {

    /**
     * 집계 1행. DAY: key=일자·chargeSum·paymentSum·count / MERCHANT: key=매장명·paymentSum·count(chargeSum=0).
     */
    public record StatRow(
            String key,
            long chargeSum,
            long paymentSum,
            long count) {
    }
}
