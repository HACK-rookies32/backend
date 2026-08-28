package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.qrpay.repository.AdminPaymentProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** 관리자 매장결제 목록 페이지 — 기존 페이징 봉투와 동일 형태. */
@Schema(description = "관리자 매장결제 목록 페이지")
public record AdminPaymentPageResponse(
        @Schema(description = "매장결제 목록") List<AdminPaymentRow> content,
        @Schema(description = "현재 페이지 번호(0부터)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "12") long totalElements,
        @Schema(description = "전체 페이지 수", example = "1") int totalPages) {

    public static AdminPaymentPageResponse from(Page<AdminPaymentProjection> page) {
        return new AdminPaymentPageResponse(
                page.getContent().stream().map(AdminPaymentRow::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
