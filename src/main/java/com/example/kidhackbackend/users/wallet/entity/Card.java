package com.example.kidhackbackend.users.wallet.entity;

import com.example.kidhackbackend.global.crypto.EncryptedStringConverter;
import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * 루키즈페이 결제수단 카드(안전 버전). 역할표 추가요청 — 카드 CRD.
 *
 * <p>1인당 여러 개. 소유자는 로그인 사용자({@code ownerId} = member.id)로 저장해 본인 카드만 관리한다.
 * 민감정보(카드번호·비번앞2·생년월일·소유자명)는 {@link EncryptedStringConverter}로 <b>저장 시 AES 암호화</b>된다.
 *
 * <p>⚠️ <b>CVC 필드는 없다</b> — 서버는 등록 시 복호만 하고 저장하지 않는다(PCI-DSS, 코드 레벨 강제).
 * {@code expiry}(MM/YY)는 민감도가 낮아 평문으로 둔다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "card", indexes = @Index(name = "idx_card_owner", columnList = "ownerId"))
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유자 = 로그인 사용자 id. 목록/삭제 시 소유권 검증 기준. */
    @Column(nullable = false)
    private Long ownerId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String cardNumber;

    @Column(nullable = false)
    private String expiry;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String passwordFront;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String birthDate;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String ownerName;

    public Card(Long ownerId, String cardNumber, String expiry,
                String passwordFront, String birthDate, String ownerName) {
        this.ownerId = ownerId;
        this.cardNumber = cardNumber;
        this.expiry = expiry;
        this.passwordFront = passwordFront;
        this.birthDate = birthDate;
        this.ownerName = ownerName;
    }

    /** 카드번호 마스킹(뒤 4자리만 노출). 응답 전용 — 원본은 반환하지 않는다. */
    public String maskedCardNumber() {
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\s", "");
        if (digits.length() < 4) {
            return "**** **** **** ****";
        }
        return "**** **** **** " + digits.substring(digits.length() - 4);
    }
}
