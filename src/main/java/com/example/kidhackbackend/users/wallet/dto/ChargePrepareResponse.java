package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import io.swagger.v3.oas.annotations.media.Schema;

/** 충전 준비 응답. 앱은 이 merchantUid 로 PG 결제 후 승인 API 를 호출한다. */
@Schema(description = "충전 준비 결과")
public record ChargePrepareResponse(
        @Schema(description = "가맹점 주문번호", example = "MRC-20260828-abc123") String merchantUid,
        @Schema(description = "충전 금액(원)", example = "10000") long amount) {

    public static ChargePrepareResponse from(ChargeOrder order) {
        return new ChargePrepareResponse(order.getMerchantUid(), order.getAmount());
    }
}
