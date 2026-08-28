package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 용돈 이체 / 잔액 회수 요청 (No.39 · No.40). 모든 필드 필수. */
@Schema(description = "용돈 이체 / 잔액 회수 요청")
public record TransferRequest(
        @Schema(description = "(필수) 대상 자녀 회원 ID", example = "7", requiredMode = RequiredMode.REQUIRED)
        @NotNull(message = "childId는 필수입니다.") Long childId,
        @Schema(description = "(필수) 이체/회수 금액(원, 1 이상)", example = "5000", requiredMode = RequiredMode.REQUIRED)
        @Positive(message = "금액은 1원 이상이어야 합니다.") long amount) {
}
