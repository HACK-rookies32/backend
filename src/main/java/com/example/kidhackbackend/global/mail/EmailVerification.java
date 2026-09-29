package com.example.kidhackbackend.global.mail;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long targetMemberId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private boolean used = false;

    public EmailVerification(Long targetMemberId, String email, String code, Purpose purpose, LocalDateTime expiresAt) {
        this.targetMemberId = targetMemberId;
        this.email = email;
        this.code = code;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    public boolean isValid(String inputCode) {
        return !used && LocalDateTime.now().isBefore(expiresAt) && this.code.equals(inputCode);
    }

    public void markUsed() {
        this.used = true;
    }

    /**
     * 인증 목적. FIND_ID·RESET_PASSWORD 는 이메일 채널(아이디찾기·비번재설정).
     * TX_OTP 는 전자금융 거래 2차 인증(SMS/ARS mock)이 재사용한다 — 별도 테이블을 두지 않고
     * 이 엔티티/테이블을 그대로 쓴다({@code email} 컬럼에 전화번호를 담는다).
     * SIGNUP_PHONE 은 회원가입 휴대폰 인증(SMS mock)이 같은 방식으로 재사용한다
     * ({@code email} 컬럼=전화번호, 가입 전이라 {@code targetMemberId}=0).
     */
    public enum Purpose { FIND_ID, RESET_PASSWORD, TX_OTP, SIGNUP_PHONE }
}