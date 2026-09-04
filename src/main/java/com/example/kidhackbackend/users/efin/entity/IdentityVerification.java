package com.example.kidhackbackend.users.efin.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 접근매체(카드 등) 발급 시 실명확인 결과. 신분증 진위확인의 판정 결과를 남긴다.
 *
 * <p>진위확인 자체는 {@code MockIdentityVerifier}(외부 진위확인 기관 mock)가 수행한다.
 *
 * <p>⚠️ [VULN-EFIN-7] 실명확인 여부를 회원이 제출한 값 기반으로만 판정하고 그 결과를 응답에 노출한다.
 * 진위확인 로직이 mock 이라 정해진 규칙만 만족하면 통과하며, 클라이언트가 verified 상태를 우회 주장할 수 있다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "identity_verification",
        indexes = @Index(name = "idx_identity_member", columnList = "memberId"))
public class IdentityVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    /** 신분증 종류 — RESIDENT(주민등록증) / DRIVER(운전면허). */
    @Column(nullable = false)
    private String idType;

    @Column(nullable = false)
    private boolean verified;

    private LocalDateTime verifiedAt;

    public IdentityVerification(Long memberId, String name, LocalDate birthDate,
                                String idType, boolean verified) {
        this.memberId = memberId;
        this.name = name;
        this.birthDate = birthDate;
        this.idType = idType;
        this.verified = verified;
        this.verifiedAt = verified ? LocalDateTime.now() : null;
    }
}