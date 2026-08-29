package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.qrpay.repository.AdminPaymentProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 관리자 매장결제 목록의 한 행 (3분할 2/3).
 *
 * <p>거래 원장에는 매장명이 없어 통합 거래내역({@code GET /finance/transactions})의 {@code merchantName} 이
 * 항상 {@code null} 이었다. 이 API 는 QR 결제 테이블을 직접 읽어 <b>실제 매장명</b>을 제공한다.
 *
 * <p><b>필드명은 모바일(회원) 응답에 맞춘다.</b> PK 는 {@code id}, 매장명은 모바일
 * {@code QrPaymentHistoryResponse.storeName} 과 같은 {@code storeName} 이다.
 * (2026-08-29 — 모바일 규약 정렬. 그전에는 {@code paymentId}/{@code merchantName} 이었다)
 */
@Schema(description = "관리자 매장결제 1행")
public record AdminPaymentRow(
        @Schema(description = "QR 결제 id", example = "5") Long id,
        @Schema(description = "결제한 자녀 회원 id", example = "4") Long childId,
        @Schema(description = "자녀 실명. 회원이 삭제됐으면 null", example = "김자녀") String childName,
        @Schema(description = "보호자 회원 id. 페어링이 없으면 null", example = "3") Long parentId,
        @Schema(description = "보호자 실명. 페어링이 없으면 null", example = "김보호") String parentName,
        @Schema(description = "매장명. 단말 인증 없이 결제되면 \"UNKNOWN\"", example = "GS25 Gangnam")
        String storeName,
        @Schema(description = "결제 금액(원)", example = "3000") Long amount,
        @Schema(description = "결제 상태", example = "PAID",
                allowableValues = {"PAID", "CANCELLED"}) String status,
        @Schema(description = "결제 직후 자녀 지갑 잔액(원). 원장을 못 찾으면 null", example = "22000")
        Long balanceAfter,
        @Schema(description = "결제 시각") LocalDateTime paidAt,
        @Schema(description = "환불 시각. 환불 이력을 별도 저장하지 않아 v1 은 항상 null") LocalDateTime refundedAt,
        @Schema(description = "환불 사유. 환불 API 에 사유 파라미터가 없어 v1 은 항상 null") String refundReason) {

    public static AdminPaymentRow from(AdminPaymentProjection p) {
        return new AdminPaymentRow(
                p.getId(),
                p.getChildId(),
                p.getChildName(),
                p.getParentId(),
                p.getParentName(),
                p.getStoreName(),
                p.getAmount(),
                p.getStatus(),
                p.getBalanceAfter(),
                p.getPaidAt(),
                null,   // refundedAt — 환불 시각 미저장
                null);  // refundReason — 환불 사유 미저장
    }
}
