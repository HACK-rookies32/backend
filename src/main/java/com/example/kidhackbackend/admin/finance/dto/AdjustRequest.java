package com.example.kidhackbackend.admin.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 잔액 수동조정 요청 (No.50). amount 양수=증액, 음수=감액.
 */
@Schema(description = "잔액 수동조정 요청")
public record AdjustRequest(
        @Schema(description = "가감 금액(원). 양수=증액, 음수=감액", example = "-5000") long amount,
        @Schema(description = "조정 사유. ⚠️ 필수여야 하나 검증 생략됨(의도된 취약점)") String reason) {
}
