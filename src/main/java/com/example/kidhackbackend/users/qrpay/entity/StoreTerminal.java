package com.example.kidhackbackend.users.qrpay.entity;

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
 * 매장 단말(POS) — QR결제 No.44 선행. 매장 단말이 결제 승인(No.44)을 호출할 때
 * 앱 JWT 대신 {@code X-Store-Terminal-Token} 헤더로 자신을 인증하기 위한 등록 정보.
 *
 * <p>발급(issue) 시 예측 불가능한 opaque 토큰과 함께 ACTIVE로 생성되고, 폐기(revoke) 시 REVOKED로 전이한다.
 * 토큰은 재사용되는 장기 자격증명이므로 만료를 두지 않고 폐기로만 무효화한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "store_terminal",
        indexes = @Index(name = "idx_store_terminal_token", columnList = "terminalToken")
)
public class StoreTerminal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 매장 단말 인증 토큰(X-Store-Terminal-Token). 발급 시 서버가 만든 예측 불가 값. */
    @Column(nullable = false, unique = true)
    private String terminalToken;

    /** 가맹점(매장) 이름. */
    @Column(nullable = false)
    private String storeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TerminalStatus status;

    public StoreTerminal(String terminalToken, String storeName) {
        this.terminalToken = terminalToken;
        this.storeName = storeName;
        this.status = TerminalStatus.ACTIVE;
    }

    /** 단말 토큰을 폐기 상태로 전이한다. */
    public void revoke() {
        this.status = TerminalStatus.REVOKED;
    }
}
