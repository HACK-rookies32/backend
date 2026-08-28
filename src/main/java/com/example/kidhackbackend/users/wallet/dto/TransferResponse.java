package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 이체/회수 응답 (No.39 · No.40). 보내는 쪽·받는 쪽 잔액과 이름을 함께 담는다. */
@Schema(description = "이체/회수 결과")
public record TransferResponse(
        @Schema(description = "보내는 지갑 소유자 ID", example = "1") Long fromOwnerId,
        @Schema(description = "보내는 사람 이름", example = "홍길동") String fromOwnerName,
        @Schema(description = "받는 지갑 소유자 ID", example = "7") Long toOwnerId,
        @Schema(description = "받는 사람 이름", example = "김하늘") String toOwnerName,
        @Schema(description = "이체 금액(원)", example = "5000") long amount,
        @Schema(description = "이체 후 보내는 쪽 잔액(원)", example = "10000") long fromBalance,
        @Schema(description = "이체 후 받는 쪽 잔액(원)", example = "5000") long toBalance) {
}
