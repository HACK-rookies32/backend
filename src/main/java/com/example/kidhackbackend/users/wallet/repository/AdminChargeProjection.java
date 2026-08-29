package com.example.kidhackbackend.users.wallet.repository;

import java.time.LocalDateTime;

/**
 * 관리자 충전 목록 projection — 충전 주문 + 주문자 회원 정보를 한 번에 가져온다.
 *
 * <p>거래 원장({@code WalletTransaction})에는 <b>주문 상태와 PG 거래 ID 가 없다</b>. 원장은 "얼마가
 * 오갔는가"만 남기기 때문에, 통합 거래내역({@code GET /finance/transactions})으로는 "이 충전이 아직
 * READY 인가, 만료됐는가, 취소됐는가"를 알 수 없다. 이 API 가 {@code charge_order} 를 직접 읽어 채운다.
 *
 * <p>{@code ChargeOrder} 가 {@code Member} 를 FK 로 물지 않고 {@code ownerId}(Long)만 들고 있어
 * 파생 쿼리로는 조인이 안 된다 → 네이티브 쿼리 + 이 projection 조합을 쓴다({@code AdminPaymentProjection}
 * 과 같은 사정).
 */
public interface AdminChargeProjection {

    /** 충전 주문 id. */
    Long getId();

    /**
     * 가맹점 주문번호({@code kid_} 접두사).
     *
     * <p>관리자 충전취소({@code POST /finance/charges/{merchantUid}/cancel})가 이 값을 경로로 받는다.
     * 목록에서 바로 취소를 걸 수 있어야 하므로 반드시 함께 내려준다 — 없으면 프론트가 거래내역의
     * {@code refUid} 를 따로 뒤져야 해서 탭 전용 API 를 둔 취지가 무너진다.
     */
    String getMerchantUid();

    /** 주문자 회원 id. */
    Long getUserId();

    /** 주문자 실명. 회원이 삭제됐으면 null. */
    String getUserName();

    /** 주문자 이메일. CHILD 는 가입 시 선택이라 null 일 수 있다. */
    String getUserEmail();

    /** 충전 금액(원). */
    Long getAmount();

    /** {@code READY / PAID / FAILED / REFUNDED / CANCELLED / EXPIRED}. */
    String getStatus();

    /** PG 승인 거래 ID(mock: {@code imp_...}). 승인 전에는 null. */
    String getPgTransactionId();

    /** 주문 생성(충전 준비) 시각. */
    LocalDateTime getCreatedAt();

    /** 환불 시각. REFUNDED 주문이면 경로와 무관하게 값이 있다(모바일과 같은 의미). */
    LocalDateTime getRefundedAt();

    /** 환불 사유. 부모가 직접 환불한 건은 사유를 받지 않아 null 이다. */
    String getRefundReason();
}
