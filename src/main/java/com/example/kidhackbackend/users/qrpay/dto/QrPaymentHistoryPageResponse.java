package com.example.kidhackbackend.users.qrpay.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 자녀 결제 내역 페이징 응답 (No.45). 팀 `ActivityPageResponse` 형태.
 */
public record QrPaymentHistoryPageResponse(
        List<QrPaymentHistoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static QrPaymentHistoryPageResponse from(Page<QrPaymentHistoryResponse> page) {
        return new QrPaymentHistoryPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
