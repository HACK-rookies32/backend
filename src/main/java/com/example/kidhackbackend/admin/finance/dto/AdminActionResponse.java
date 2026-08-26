package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.wallet.dto.TxResponse;

/**
 * 관리자 자금 변경(강제환불 No.48 / 잔액조정 No.50) 결과 응답. 대상 회원·조정 후 잔액·기록된 원장 1건.
 */
public record AdminActionResponse(
        Long targetUserId,
        long balance,
        TxResponse transaction) {
}
