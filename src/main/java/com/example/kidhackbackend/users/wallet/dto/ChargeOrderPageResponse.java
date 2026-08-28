package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** 충전 주문 페이지 (No.37). */
@Schema(description = "충전 주문 페이지")
public record ChargeOrderPageResponse(
        @Schema(description = "충전 주문 목록") List<ChargeOrderResponse> content,
        @Schema(description = "현재 페이지 번호(0부터)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "12") long totalElements,
        @Schema(description = "전체 페이지 수", example = "1") int totalPages) {

    public static ChargeOrderPageResponse from(Page<ChargeOrder> page) {
        return new ChargeOrderPageResponse(
                page.getContent().stream().map(ChargeOrderResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
