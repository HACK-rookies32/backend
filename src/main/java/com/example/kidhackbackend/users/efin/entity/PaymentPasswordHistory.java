package com.example.kidhackbackend.users.efin.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 비밀번호 변경 이력. 과거 비밀번호 재사용 차단에 쓴다.
 *
 * <p>⚠️ [VULN-EFIN-6] 이력을 <b>평문</b>으로 저장한다. 재사용 차단을 위해 과거 값을 대조해야 한다는
 * 이유로 원문을 그대로 쌓아, DB 가 탈취되면 한 회원이 지금까지 쓴 결제 비밀번호가 전부 노출된다.
 * (재사용 방지 기능을 넣으면서 오히려 더 큰 정보노출을 만든 전형적 안티패턴 — VULN-1 계열)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment_password_history",
        indexes = @Index(name = "idx_pay_pw_hist_member", columnList = "memberId"))
public class PaymentPasswordHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    /** [VULN-EFIN-6] 평문 비밀번호. */
    @Column(nullable = false)
    private String password;

    public PaymentPasswordHistory(Long memberId, String password) {
        this.memberId = memberId;
        this.password = password;
    }
}
