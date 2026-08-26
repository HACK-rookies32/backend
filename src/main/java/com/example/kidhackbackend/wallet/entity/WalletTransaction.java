package com.example.kidhackbackend.wallet.entity;

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
 * 지갑 원장(ledger) 한 줄 — 역할표 No.35.
 *
 * <p>잔액 변동이 생길 때마다 1건 기록한다. {@code balanceAfter}로 변동 후 잔액을 스냅샷하고,
 * {@code refUid}로 근거 주문(merchantUid)을 연결한다. 충전내역 조회(No.37)의 원천 데이터.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "wallet_transaction",
        indexes = @Index(name = "idx_wallet_tx_wallet", columnList = "walletId,id")
)
public class WalletTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TxType type;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private long balanceAfter;

    /** 근거 참조(충전 주문의 merchantUid 등). */
    private String refUid;

    public WalletTransaction(Long walletId, TxType type, long amount, long balanceAfter, String refUid) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.refUid = refUid;
    }
}
