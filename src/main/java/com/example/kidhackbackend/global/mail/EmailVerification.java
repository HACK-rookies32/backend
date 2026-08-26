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

    public enum Purpose { FIND_ID, RESET_PASSWORD }
}