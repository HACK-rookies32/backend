package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.wallet.repository.AdminTxProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** 관리자 전체 거래 내역 페이징 응답 (No.47). */
@Schema(description = "관리자 전체 거래 내역 페이지")
public record AdminTxPageResponse(
        @Schema(description = "거래 원장 목록(회원 정보 포함)") List<AdminTxRow> content,
        @Schema(description = "현재 페이지 번호(0부터)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "128") long totalElements,
        @Schema(description = "전체 페이지 수", example = "7") int totalPages,
        @Schema(description = "userId 로 조회했을 때만 채워지는 해당 회원 잔액(전체 조회 시 null)", example = "15000")
        Long walletBalance) {

    public static AdminTxPageResponse from(Page<AdminTxProjection> page) {
        return from(page, null);
    }

    public static AdminTxPageResponse from(Page<AdminTxProjection> page, Long walletBalance) {
        return new AdminTxPageResponse(
                page.getContent().stream().map(AdminTxRow::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                walletBalance);
    }
}
