package com.example.kidhackbackend.users.qrpay.repository;

import java.time.LocalDateTime;

/**
 * 관리자 매장결제 목록 projection — QR 결제 + 자녀/보호자 + 결제 후 잔액을 한 번에 가져온다.
 *
 * <p>거래 원장({@code WalletTransaction})에는 <b>매장명이 없다</b>. 매장명은 {@code QrPayment.storeName} 에만
 * 있어서, 통합 거래내역({@code GET /finance/transactions})에서는 {@code merchantName} 을 채울 수 없었다.
 * 이 API 가 QR 결제 테이블을 직접 읽어 그 값을 제공한다.
 *
 * <p>{@code balanceAfter} 는 원장에서 가져온다 — 같은 {@code qrToken} 에 PAYMENT 원장이 여러 건일 수 있어
 * ({@code [VULN-WALLET-16]} QR 재사용) <b>가장 최근 1건</b>을 상관 서브쿼리로 고른다.
 */
public interface AdminPaymentProjection {

    /** QR 결제 id. */
    Long getPaymentId();

    Long getChildId();

    /** 자녀 실명. 회원이 삭제됐으면 null. */
    String getChildName();

    /** 보호자 회원 id. 페어링이 없으면 null. */
    Long getParentId();

    /** 보호자 실명. 페어링이 없으면 null. */
    String getParentName();

    /** 매장명 — {@code QrPayment.storeName}. 단말 인증 없이 결제되면 "UNKNOWN". */
    String getMerchantName();

    Long getAmount();

    /** {@code PAID} / {@code CANCELLED}. */
    String getStatus();

    /** 결제 직후 자녀 지갑 잔액. 원장을 못 찾으면 null. */
    Long getBalanceAfter();

    LocalDateTime getPaidAt();
}
