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
import java.time.LocalDateTime;
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

    /**
     * 환불 사유. 관리자 충전취소(#139)로 되돌린 경우에만 값이 있다.
     *
     * <p>감사로그에도 같은 사유가 남지만 목적이 다르다 — 감사로그는 <b>누가 했는가</b>(부인방지)를 남기는
     * append-only 기록이고, 이 필드는 <b>주문이 어떻게 됐는가</b>라는 주문 자신의 상태다. 목록 조회가
     * 감사로그를 뒤지지 않아도 되게 여기에 둔다.
     *
     * <p>부모가 직접 환불한 건({@code WalletRefundService})은 사유를 받지 않으므로 {@code null} 로 남는다.
     * → 이 필드의 유무로 <b>관리자 취소와 부모 환불이 구분</b>된다.
     */
    @Column(length = 500)
    private String refundReason;

    /**
     * 환불 시각. 주문이 REFUNDED 로 전이한 시점이다.
     *
     * <p><b>사유와 달리 두 경로 모두 채워진다</b> — 모바일 충전내역
     * ({@code ChargeHistoryResponse.refundedAt})이 "모든 REFUNDED 주문의 환불 시각"을 뜻하므로
     * 같은 이름이 다른 의미가 되지 않게 맞춘다. 관리자 취소만 값이 있는 것은 {@link #refundReason} 이다.
     */
    private LocalDateTime refundedAt;

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

    /**
     * 환불 처리로 주문을 REFUNDED 상태로 전이한다. (No.37) 부모 환불 경로 — 사유를 받지 않는다.
     *
     * <p>사유는 없어도 <b>환불 시각은 남긴다.</b> 모바일 충전내역이 지금은 REFUND 원장을 주문마다
     * 조회해 이 값을 유도하는데(N+1), 컬럼에 남겨두면 그 조회를 없앨 수 있다.
     */
    public void markRefunded() {
        this.status = ChargeStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    /**
     * 관리자 충전취소로 주문을 REFUNDED 로 전이하면서 사유·시각을 함께 남긴다. (#139 · #145)
     *
     * @param reason 관리자가 입력한 취소 사유. 컨트롤러의 {@code @NotBlank} 가 공백을 막는다
     */
    public void markRefunded(String reason) {
        this.status = ChargeStatus.REFUNDED;
        this.refundReason = reason;
        this.refundedAt = LocalDateTime.now();
    }

    /** PG 결제 실패/취소 웹훅으로 주문을 FAILED 상태로 전이한다. (No.36) */
    public void markFailed() {
        this.status = ChargeStatus.FAILED;
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
