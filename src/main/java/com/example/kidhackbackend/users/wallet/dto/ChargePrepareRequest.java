package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/** 충전 준비 요청. */
@Schema(description = "충전 준비 요청")
public record ChargePrepareRequest(
        @Schema(description = "충전 금액(원, 양수)", example = "10000")
        @Positive(message = "충전 금액은 1원 이상이어야 합니다.") long amount) {
}
