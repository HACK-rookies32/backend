package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 관리자 자금 변경(강제환불 No.48 / 잔액조정 No.50) 결과. */
@Schema(description = "관리자 자금 변경 결과")
public record AdminActionResponse(
        @Schema(description = "대상 회원 ID", example = "7") Long targetUserId,
        @Schema(description = "조정 후 잔액(원)", example = "10000") long balance,
        @Schema(description = "기록된 원장 1건") TxResponse transaction) {
}
