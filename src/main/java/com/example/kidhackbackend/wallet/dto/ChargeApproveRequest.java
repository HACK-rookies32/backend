package com.example.kidhackbackend.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 충전 승인 요청. 앱이 PG 결제 후 받은 정보를 서버로 전달한다.
 *
 * @param merchantUid     준비 단계에서 받은 가맹점 주문번호
 * @param amount          결제 금액(원)
 * @param pgTransactionId PG 거래 ID(mock 환경에서는 없어도 됨)
 */
public record ChargeApproveRequest(
        @NotBlank(message = "merchantUid는 필수입니다.") String merchantUid,
        @Positive(message = "결제 금액은 1원 이상이어야 합니다.") long amount,
        String pgTransactionId) {
}
