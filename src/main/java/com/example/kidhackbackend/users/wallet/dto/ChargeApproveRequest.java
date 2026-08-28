package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** 충전 승인 요청. 앱이 PG 결제 후 받은 정보를 서버로 전달한다. */
@Schema(description = "충전 승인 요청")
public record ChargeApproveRequest(
        @Schema(description = "(필수) 준비 단계에서 받은 가맹점 주문번호", example = "kid_ab12cd34",
                requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "merchantUid는 필수입니다.") String merchantUid,
        @Schema(description = "(필수) 결제 금액(원, 1 이상)", example = "10000", requiredMode = RequiredMode.REQUIRED)
        @Positive(message = "결제 금액은 1원 이상이어야 합니다.") long amount,
        @Schema(description = "(선택) PG 거래 ID (mock 환경에서는 생략 가능)") String pgTransactionId) {
}
