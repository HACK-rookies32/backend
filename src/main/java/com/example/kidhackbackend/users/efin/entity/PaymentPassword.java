package com.example.kidhackbackend.users.efin.entity;

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
 * 결제 비밀번호. 회원당 1개(member_id unique).
 *
 * <p>로그인 비밀번호와 분리된 거래용 4자리 PIN 이다. 접근매체(카드) 발급 시 최초 설정하고, 송금·회수·충전
 * 등 결제 승인 시 본인확인에 쓴다.
 *
 * <p>⚠️ [VULN-EFIN-5] 해시가 <b>salt 없는 MD5</b>({@code WeakHashUtil.md5})다. 4자리 숫자 PIN 이라
 * 레인보우테이블/브루트포스로 즉시 역산된다. 로그인 비번 평문 저장(VULN-1)·refresh token MD5(VULN-AUTH-RT)와
 * 동일 계열의 약한 저장.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment_password")
public class PaymentPassword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유 회원 id. 회원당 1개. */
    @Column(nullable = false, unique = true)
    private Long memberId;

    // [재점검 2026-09-07] 취약 확정 — 금취분평 해당 항목 없음(저장 비밀번호 해시 전용 항목 부재)·주통기 해당 항목 없음(웹앱 21개에 저장해시 항목 없음) → "해당 없지만 취약점".
    //   4자리 PIN 을 salt 없는 MD5 로 저장 → DB 유출 시 레인보우/브루트포스로 즉시 역산(약한 저장 암호).
    /** [VULN-EFIN-5] salt 없는 MD5 해시. */
    @Column(nullable = false)
    private String passwordHash;

    public PaymentPassword(Long memberId, String passwordHash) {
        this.memberId = memberId;
        this.passwordHash = passwordHash;
    }

    /** 비밀번호 변경 — 해시를 교체한다(감사시각은 BaseEntity.updatedAt 이 자동 기록). */
    public void changeHash(String newHash) {
        this.passwordHash = newHash;
    }

    public boolean matches(String candidateHash) {
        return this.passwordHash.equals(candidateHash);
    }
}
