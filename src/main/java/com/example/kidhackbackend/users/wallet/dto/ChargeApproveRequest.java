package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** 충전 승인 요청. 앱이 PG 결제 후 받은 정보를 서버로 전달한다. */
@Schema(description = "충전 승인 요청")
public record ChargeApproveRequest(
        @Schema(description = "준비 단계에서 받은 가맹점 주문번호", example = "MRC-20260828-abc123")
        @NotBlank(message = "merchantUid는 필수입니다.") String merchantUid,
        @Schema(description = "결제 금액(원)", example = "10000")
        @Positive(message = "결제 금액은 1원 이상이어야 합니다.") long amount,
        @Schema(description = "PG 거래 ID (mock 환경에서는 생략 가능)") String pgTransactionId) {
}
