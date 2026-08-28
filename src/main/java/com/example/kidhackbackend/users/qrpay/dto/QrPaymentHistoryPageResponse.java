package com.example.kidhackbackend.users.qrpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** 자녀 결제 내역 페이징 응답 (No.45). */
@Schema(description = "자녀 결제 내역 페이지")
public record QrPaymentHistoryPageResponse(
        @Schema(description = "결제 내역 목록") List<QrPaymentHistoryResponse> content,
        @Schema(description = "현재 페이지 번호(0부터)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "5") long totalElements,
        @Schema(description = "전체 페이지 수", example = "1") int totalPages) {

    public static QrPaymentHistoryPageResponse from(Page<QrPaymentHistoryResponse> page) {
        return new QrPaymentHistoryPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
