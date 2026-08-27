package com.example.kidhackbackend.users.wallet.entity;

/**
 * 지갑 원장(WalletTransaction) 거래 유형.
 *
 * <p>충전(CHARGE)·환불(REFUND)·이체(TRANSFER_IN/OUT)·매장결제(PAYMENT/PAYMENT_CANCEL)를 사용한다.
 * TRANSFER_OUT은 보내는 지갑, TRANSFER_IN은 받는 지갑에 기록한다. (용돈 이체 No.39·잔액 회수 No.40)
 * PAYMENT는 QR 스캔 매장결제로 자녀 지갑을 차감(No.44), PAYMENT_CANCEL은 그 취소로 자녀 지갑에 환급할 때(No.46) 기록한다.
 * ADJUST는 관리자 잔액 수동조정(No.50) 시 기록한다(가감분, 음수 가능).
 */
public enum TxType {
    CHARGE,
    REFUND,
    TRANSFER_IN,
    TRANSFER_OUT,
    PAYMENT,
    PAYMENT_CANCEL,
    ADJUST
}
