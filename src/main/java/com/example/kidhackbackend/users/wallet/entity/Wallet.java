package com.example.kidhackbackend.users.wallet.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 지갑 — 결제/지갑 대영역의 잔액 보관 엔티티. 역할표 No.35.
 *
 * <p>회원 1인당 1개(ownerId unique). 잔액은 원(KRW) 단위 {@code long}으로 관리한다.
 * 충전(No.35)·이체(No.39)·회수(No.40)가 이 잔액을 갱신한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "wallet")
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long ownerId;

    @Column(nullable = false)
    private long balance;

    public Wallet(Long ownerId) {
        this.ownerId = ownerId;
        this.balance = 0L;
    }

    /** 잔액을 amount만큼 증가시킨다. (충전/이체 입금 공통) */
    public void credit(long amount) {
        // Math.addExact: 오버플로우 시 조용히 음수로 돌지 않고 ArithmeticException(→400)으로 끊는다(#35).
        // 정상 금액은 영향 없다. 관리자 조정의 '거대 금액 자금 창출'(VULN-ADMIN-2)은 표적이라 그대로 통과된다.
        this.balance = Math.addExact(this.balance, amount);
    }

    /** 잔액을 amount만큼 차감한다. (환불/이체 출금 공통) */
    public void debit(long amount) {
        this.balance = Math.subtractExact(this.balance, amount);
    }
}
