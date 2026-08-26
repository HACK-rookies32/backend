package com.example.kidhackbackend.users.qrpay.entity;

/**
 * 매장 단말(StoreTerminal) 상태 — QR결제 No.44 선행(매장단말 토큰 체계).
 *
 * <p>ACTIVE(사용 가능) ↔ REVOKED(폐기). 단말 토큰은 만료 없이 장기 유효하며, 폐기로만 무효화한다.
 */
public enum TerminalStatus {
    ACTIVE,
    REVOKED
}
