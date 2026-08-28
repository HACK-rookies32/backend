package com.example.kidhackbackend.users.wallet.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 충전 주문(결제 인텐트) — 역할표 No.35, 2단계 결제의 '준비' 산출물.
 *
 * <p>준비(prepare) 단계에서 {@code merchantUid}와 함께 READY 상태로 생성되고,
 * 승인(approve) 단계에서 PG 승인 결과({@code pgTransactionId})와 함께 PAID로 전이한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "charge_order",
        indexes = @Index(name = "idx_charge_order_merchant", columnList = "merchantUid")
)
public class ChargeOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 가맹점 주문번호. 준비 시 서버가 발급하고, 승인 요청이 이 값으로 주문을 특정한다. */
    @Column(nullable = false, unique = true)
    private String merchantUid;

    /** 주문을 만든 회원(지갑 소유자). */
    @Column(nullable = false)
    private Long ownerId;

    /** 준비 시 확정된 결제 금액(원). 승인 시 이 값이 '정답'이어야 한다. */
    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeStatus status;

    /** PG 승인 거래 ID(mock: imp_...). 승인 전에는 null. */
    private String pgTransactionId;

    public ChargeOrder(String merchantUid, Long ownerId, long amount) {
        this.merchantUid = merchantUid;
        this.ownerId = ownerId;
        this.amount = amount;
        this.status = ChargeStatus.READY;
    }

    public void markPaid(String pgTransactionId) {
        this.status = ChargeStatus.PAID;
        this.pgTransactionId = pgTransactionId;
    }

    /** 환불 처리로 주문을 REFUNDED 상태로 전이한다. (No.37) */
    public void markRefunded() {
        this.status = ChargeStatus.REFUNDED;
    }

    /** 사용자가 결제 진행 중 취소 — READY 주문만 CANCELLED 로 전이한다. */
    public void markCancelled() {
        this.status = ChargeStatus.CANCELLED;
    }

    /** approve 없이 방치된 READY 주문을 EXPIRED 로 전이한다. (만료 배치) */
    public void markExpired() {
        this.status = ChargeStatus.EXPIRED;
    }
}
