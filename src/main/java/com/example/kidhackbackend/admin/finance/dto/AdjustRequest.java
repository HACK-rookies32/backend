package com.example.kidhackbackend.admin.finance.dto;

/**
 * 잔액 수동조정 요청 (No.50).
 *
 * @param amount 가감 금액(원). 양수=증액, 음수=감액.
 * @param reason 조정 사유(역할표상 필수이나 [VULN-ADMIN-2]로 검증 생략).
 */
public record AdjustRequest(
        long amount,
        String reason) {
}
