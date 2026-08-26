package com.example.kidhackbackend.users.wallet.dto;

import jakarta.validation.constraints.Positive;

/**
 * 충전 준비 요청.
 *
 * @param amount 충전 금액(원). 양수만 허용.
 */
public record ChargePrepareRequest(
        @Positive(message = "충전 금액은 1원 이상이어야 합니다.") long amount) {
}
