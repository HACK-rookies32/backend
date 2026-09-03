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
 * 전자금융 별도 비밀번호(계좌비밀번호). 회원당 1개(member_id unique).
 *
 * <p>로그인 비밀번호와 분리된 거래용 비밀번호 체계다. 송금·회수 등 거래 승인 시 본인확인에 쓴다.
 *
 * <p>⚠️ [VULN-EFIN-5] 해시가 <b>salt 없는 MD5</b>({@code WeakHashUtil.md5})다. 6자리 숫자 비번이라
 * 레인보우테이블/브루트포스로 즉시 역산된다. 로그인 비번 평문 저장(VULN-1)·refresh token MD5(VULN-AUTH-RT)와
 * 동일 계열의 약한 저장.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "account_password")
public class AccountPassword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유 회원 id. 회원당 1개. */
    @Column(nullable = false, unique = true)
    private Long memberId;

    /** [VULN-EFIN-5] salt 없는 MD5 해시. */
    @Column(nullable = false)
    private String passwordHash;

    public AccountPassword(Long memberId, String passwordHash) {
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