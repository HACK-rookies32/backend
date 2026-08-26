package com.example.kidhackbackend.users.wallet.entity;

/**
 * 지갑 원장(WalletTransaction) 거래 유형.
 *
 * <p>충전(CHARGE)·환불(REFUND)을 사용하며, 이후 용돈 이체(No.39)·잔액 회수(No.40)에서
 * TRANSFER_IN/OUT 등으로 값을 확장한다.
 */
public enum TxType {
    CHARGE,
    REFUND
}
