package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;

/**
 * 충전 준비 응답. 앱은 이 {@code merchantUid}로 PG 결제를 진행한 뒤 승인 API를 호출한다.
 */
public record ChargePrepareResponse(String merchantUid, long amount) {

    public static ChargePrepareResponse from(ChargeOrder order) {
        return new ChargePrepareResponse(order.getMerchantUid(), order.getAmount());
    }
}
