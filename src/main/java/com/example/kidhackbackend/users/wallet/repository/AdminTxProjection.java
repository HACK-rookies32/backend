package com.example.kidhackbackend.users.wallet.repository;

import java.time.LocalDateTime;

/**
 * 관리자 거래내역 조회용 projection — 원장 + 소유 회원 정보를 한 번의 쿼리로 가져온다.
 *
 * <p>{@code Wallet} 이 {@code Member} 를 FK 로 물지 않고 {@code ownerId}(Long)만 들고 있어 파생 쿼리로는
 * 조인할 수 없다. 그래서 {@link WalletTransactionRepository#adminSearchWithMember} 가 세 엔티티를 명시적으로
 * 조인해 이 형태로 내려준다. <b>행마다 회원을 다시 조회하지 않기 위한 것</b>(N+1 방지)이다.
 */
public interface AdminTxProjection {

    Long getId();

    String getType();

    long getAmount();

    long getBalanceAfter();

    String getRefUid();

    LocalDateTime getCreatedAt();

    /** 거래 주체 회원 id — {@code Wallet.ownerId}. */
    Long getUserId();

    /** 거래 주체 실명 — {@code Member.username}. */
    String getUserName();
}
