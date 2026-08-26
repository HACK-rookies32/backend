package com.example.kidhackbackend.users.wallet.entity;

/**
 * 지갑 원장(WalletTransaction) 거래 유형.
 *
 * <p>현재는 충전(CHARGE)만 사용하며, 이후 용돈 이체(No.39)·잔액 회수(No.40)·
 * 취소/환불(No.37)에서 TRANSFER_IN/OUT·REFUND 등으로 값을 확장한다.
 */
public enum TxType {
    CHARGE
}
