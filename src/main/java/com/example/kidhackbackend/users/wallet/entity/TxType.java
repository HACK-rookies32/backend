package com.example.kidhackbackend.users.wallet.entity;

/**
 * 지갑 원장(WalletTransaction) 거래 유형.
 *
 * <p>충전(CHARGE)·환불(REFUND)·이체(TRANSFER_IN/OUT)·매장결제(PAYMENT)를 사용한다.
 * TRANSFER_OUT은 보내는 지갑, TRANSFER_IN은 받는 지갑에 기록한다. (용돈 이체 No.39·잔액 회수 No.40)
 * PAYMENT는 QR 스캔 매장결제로 자녀 지갑을 차감할 때 기록한다. (No.44) (취소 시 PAYMENT_CANCEL은 No.46에서 추가 예정)
 */
public enum TxType {
    CHARGE,
    REFUND,
    TRANSFER_IN,
    TRANSFER_OUT,
    PAYMENT
}
