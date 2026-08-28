package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.wallet.repository.AdminTxProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 관리자 거래내역 목록의 한 행 (No.47, 프론트 3차 3번).
 *
 * <p>회원용 {@code TxResponse} 와 분리한 <b>관리자 전용</b> DTO다. {@code TxResponse} 는 내 지갑 조회·자녀 지갑·
 * 결제 승인 응답 등 16곳에서 공유되므로, 거기에 회원 정보를 넣으면 회원 응답까지 오염된다.
 * 관리자 목록만 회원 식별자·실명을 함께 내려준다.
 */
@Schema(description = "관리자 거래내역 1행 (회원 정보 포함)")
public record AdminTxRow(
        @Schema(description = "원장 ID", example = "12") Long id,

        @Schema(description = "거래 주체 회원 id", example = "3") Long userId,

        @Schema(description = "거래 주체 실명. 탈퇴 등으로 회원을 찾지 못하면 null", example = "김보호")
        String userName,

        @Schema(description = "거래 유형 — CHARGE(충전,증가) / REFUND(환불) / TRANSFER_IN·OUT(용돈 이체) / "
                + "PAYMENT(매장결제,차감) / PAYMENT_CANCEL(결제취소,증가) / ADJUST(관리자 조정)",
                example = "CHARGE") String type,

        @Schema(description = "거래 금액(원)", example = "50000") long amount,

        @Schema(description = "거래 후 잔액(원)", example = "50000") long balanceAfter,

        @Schema(description = "거래 유형별 참조 키 — CHARGE·REFUND(충전계열)=merchantUid, "
                + "PAYMENT·PAYMENT_CANCEL=qrToken, TRANSFER_IN·OUT=상대 회원 id, ADJUST=조정 사유. "
                + "⚠️ 관리자 강제환불의 REFUND 는 예외적으로 qrToken",
                example = "kid_ab12cd") String refUid,

        @Schema(description = "매장명 — PAYMENT 계열에서만 채워진다. v1 미제공(null)", example = "null")
        String merchantName,

        @Schema(description = "결제수단 — CHARGE 계열 표시용. 현재 결제수단 정보를 저장하지 않아 고정값",
                example = "카드") String method,

        @Schema(description = "거래 시각") LocalDateTime createdAt) {

    /** CHARGE 계열에 표시할 결제수단. 실제 수단 정보는 저장하지 않아(MockPG) 고정값을 쓴다. */
    private static final String DEFAULT_METHOD = "카드";

    public static AdminTxRow from(AdminTxProjection p) {
        boolean chargeLike = "CHARGE".equals(p.getType()) || "REFUND".equals(p.getType());
        return new AdminTxRow(
                p.getId(),
                p.getUserId(),
                p.getUserName(),
                p.getType(),
                p.getAmount(),
                p.getBalanceAfter(),
                p.getRefUid(),
                null,                                   // merchantName — v1 미제공
                chargeLike ? DEFAULT_METHOD : null,     // method — CHARGE 계열만
                p.getCreatedAt());
    }
}
