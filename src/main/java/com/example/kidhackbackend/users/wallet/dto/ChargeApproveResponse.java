package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 충전 승인 응답. 승인 후 잔액과 방금 적립된 원장 1건. */
@Schema(description = "충전 승인 결과")
public record ChargeApproveResponse(
        @Schema(description = "가맹점 주문번호") String merchantUid,
        @Schema(description = "승인 후 잔액(원)", example = "15000") long balance,
        @Schema(description = "방금 적립된 원장 1건") TxResponse transaction) {
}
