package com.example.kidhackbackend.users.wallet.dto;

/**
 * 충전 승인 응답. 승인 후 잔액과 방금 적립된 원장 1건을 담는다.
 */
public record ChargeApproveResponse(String merchantUid, long balance, TxResponse transaction) {
}
