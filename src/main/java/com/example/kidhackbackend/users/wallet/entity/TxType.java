package com.example.kidhackbackend.users.wallet.entity;

/**
 * 지갑 원장(WalletTransaction) 거래 유형.
 *
 * <p>충전(CHARGE)·환불(REFUND)·이체(TRANSFER_IN/OUT)를 사용한다.
 * TRANSFER_OUT은 보내는 지갑, TRANSFER_IN은 받는 지갑에 기록한다. (용돈 이체 No.39·잔액 회수 No.40)
 */
public enum TxType {
    CHARGE,
    REFUND,
    TRANSFER_IN,
    TRANSFER_OUT
}
