package com.example.kidhackbackend.users.wallet.dto;

import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.pg.PgCheckout;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 충전 준비 응답. 앱은 이 정보로 결제를 진행한 뒤 승인 API 를 호출한다.
 *
 * <p>{@code mock=true} 면 결제창 단계가 없으므로 곧바로 승인(approve)을 호출하면 된다.
 */
@Schema(description = "충전 준비 결과")
public record ChargePrepareResponse(
        @Schema(description = "가맹점 주문번호", example = "kid_ab12cd34") String merchantUid,
        @Schema(description = "충전 금액(원)", example = "10000") long amount,
        @Schema(description = "PG 사 식별자", example = "MOCK") String pgProvider,
        @Schema(description = "결제창 URL (mock 이면 null)", nullable = true) String checkoutUrl,
        @Schema(description = "모의 결제 여부 — true 면 결제창 없이 바로 approve 호출", example = "true") boolean mock) {

    public static ChargePrepareResponse from(ChargeOrder order, PgCheckout checkout) {
        return new ChargePrepareResponse(
                order.getMerchantUid(),
                order.getAmount(),
                checkout.provider(),
                checkout.checkoutUrl(),
                checkout.mock());
    }
}
