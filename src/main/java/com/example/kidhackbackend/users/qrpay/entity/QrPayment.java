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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 QR(결제 인텐트) — QR결제 No.42. 자녀가 발급하는 1회성 결제 토큰.
 *
 * <p>발급(No.42) 시 ISSUED 상태와 60초 만료(expiresAt)로 생성된다. 금액·매장명은 발급 시점엔 없고,
 * 매장 단말이 스캔해 승인(No.44)할 때 확정된다(그때 이 엔티티에 금액/매장/단말 정보가 추가될 예정).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "qr_payment",
        indexes = @Index(name = "idx_qr_payment_token", columnList = "qrToken")
)
public class QrPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** QR 결제 토큰. 매장 단말이 스캔해 No.44에서 이 값으로 결제를 특정한다. */
    @Column(nullable = false, unique = true)
    private String qrToken;

    /** QR을 발급한 자녀(결제될 지갑 소유자). */
    @Column(nullable = false)
    private Long childId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QrStatus status;

    /** 만료 시각(발급 + 60초). 이후 승인 불가. */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public QrPayment(String qrToken, Long childId, LocalDateTime expiresAt) {
        this.qrToken = qrToken;
        this.childId = childId;
        this.status = QrStatus.ISSUED;
        this.expiresAt = expiresAt;
    }

    /** 주어진 시각 기준 만료 여부. */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }
}
