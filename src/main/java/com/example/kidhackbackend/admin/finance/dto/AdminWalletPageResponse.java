package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.member.repository.AdminWalletProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/** 관리자 회원 지갑 목록 페이지 — 기존 {@code AdminTxPageResponse} 와 동일한 페이징 봉투. */
@Schema(description = "관리자 회원 지갑 목록 페이지")
public record AdminWalletPageResponse(
        @Schema(description = "회원 지갑 목록") List<AdminWalletRow> content,
        @Schema(description = "현재 페이지 번호(0부터)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "20") int size,
        @Schema(description = "전체 항목 수", example = "42") long totalElements,
        @Schema(description = "전체 페이지 수", example = "3") int totalPages) {

    public static AdminWalletPageResponse from(Page<AdminWalletProjection> page) {
        return new AdminWalletPageResponse(
                page.getContent().stream().map(AdminWalletRow::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
