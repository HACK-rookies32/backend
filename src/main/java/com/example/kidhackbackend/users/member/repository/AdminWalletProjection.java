package com.example.kidhackbackend.users.member.repository;

import java.time.LocalDateTime;

/**
 * 관리자 회원 지갑 목록 projection — 회원 + 지갑 잔액을 한 번의 쿼리로 가져온다.
 *
 * <p><b>회원 기준 LEFT JOIN</b> 이다. 지갑이 아직 없는 회원(한 번도 충전·이체를 받지 않은 자녀)도 목록에
 * 나와야 잔액 조정 대상으로 고를 수 있기 때문이다. 이 경우 {@code balance} 는 0, {@code updatedAt} 은 null 이다.
 *
 * <p>네이티브 쿼리 결과를 매핑하므로 컬럼 별칭이 getter 이름과 맞아야 한다
 * ({@code user_id AS userId} 형태).
 */
public interface AdminWalletProjection {

    /** 회원 id. */
    Long getUserId();

    /** 회원 실명. */
    String getUserName();

    /** {@code PARENT} / {@code CHILD}. */
    String getRole();

    /** 현재 잔액(원). 지갑이 없으면 0. */
    long getBalance();

    /** 지갑 최근 변동 시각. 지갑이 없으면 null. */
    LocalDateTime getUpdatedAt();
}
