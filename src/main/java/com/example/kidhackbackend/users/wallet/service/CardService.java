package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.global.crypto.CryptoException;
import com.example.kidhackbackend.global.crypto.HybridDecryptor;
import com.example.kidhackbackend.global.crypto.NonceStore;
import com.example.kidhackbackend.global.crypto.RsaKeyProvider;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.wallet.dto.CardPlain;
import com.example.kidhackbackend.users.wallet.dto.CardPublicKeyResponse;
import com.example.kidhackbackend.users.wallet.dto.CardResponse;
import com.example.kidhackbackend.users.wallet.dto.EncryptedCardRequest;
import com.example.kidhackbackend.users.wallet.entity.Card;
import com.example.kidhackbackend.users.wallet.repository.CardRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드 CRD(안전 버전) 서비스. 역할표 추가요청 — 금취분평 대응.
 *
 * <p>이 도메인은 <b>의도적 취약점을 심지 않는다.</b> 전송 E2E 복호 + 재전송 방지 + 저장 필드암호화
 * + CVC 미저장 + 마스킹 + 소유권 검증을 모두 갖춘다(결제/QR 도메인의 VULN-* 과 대비되는 대조군).
 */
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final RsaKeyProvider rsaKeyProvider;
    private final HybridDecryptor hybridDecryptor;
    private final NonceStore nonceStore;
    // ObjectMapper는 이 앱에서 빈으로 노출돼 있지 않아 직접 생성(PgWebhookController 관행과 동일).
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 공개키 제공 — 프론트 암호화용. keyId 동봉(회전 대비). */
    public CardPublicKeyResponse getPublicKey() {
        return new CardPublicKeyResponse(
                rsaKeyProvider.getKeyId(),
                rsaKeyProvider.getPublicKeyBase64(),
                rsaKeyProvider.getKeyRotationDays());
    }

    /**
     * 카드 등록. 복호 → 재전송 검증 → 필드암호화 저장(CVC 제외). ownerId는 JWT 사용자 id.
     */
    @Transactional
    public CardResponse create(Long ownerId, EncryptedCardRequest request) {
        String json = hybridDecryptor.decrypt(
                request.keyId(), request.encryptedKey(), request.iv(), request.ciphertext());

        CardPlain plain = parse(json);
        verifyReplay(plain);

        // CVC(plain.cvc())는 복호만 하고 저장하지 않는다 — 엔티티에 필드 자체가 없다(PCI-DSS).
        Card card = cardRepository.save(new Card(
                ownerId,
                requireField(plain.cardNumber(), "cardNumber"),
                requireField(plain.expiry(), "expiry"),
                requireField(plain.passwordFront(), "passwordFront"),
                requireField(plain.birthDate(), "birthDate"),
                requireField(plain.ownerName(), "ownerName")));

        return CardResponse.from(card);
    }

    /** 본인 카드 목록(마스킹). */
    @Transactional(readOnly = true)
    public List<CardResponse> list(Long ownerId) {
        return cardRepository.findByOwnerIdOrderByIdDesc(ownerId).stream()
                .map(CardResponse::from)
                .toList();
    }

    /**
     * 카드 삭제 — 소유권 검증. 남의 카드거나 없으면 404(존재 여부 노출 금지 = IDOR 방어).
     */
    @Transactional
    public void delete(Long requesterId, Long cardId) {
        Card card = cardRepository.findByIdAndOwnerId(cardId, requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("카드를 찾을 수 없습니다."));
        cardRepository.delete(card);
    }

    /* ------------------------------------------------------------- 내부 ---- */

    private CardPlain parse(String json) {
        try {
            return objectMapper.readValue(json, CardPlain.class);
        } catch (Exception e) {
            throw new CryptoException("카드 payload 형식이 올바르지 않습니다.", e);
        }
    }

    /** 재전송 방지: timestamp 유효창(±ttl) + nonce 미사용. 규약 확정본. */
    private void verifyReplay(CardPlain plain) {
        if (plain.nonce() == null || plain.nonce().isBlank() || plain.timestamp() == null) {
            throw new CryptoException("재전송 방지 필드(nonce/timestamp)가 없습니다.");
        }
        long now = System.currentTimeMillis();
        long ts = normalizeToMillis(plain.timestamp());
        if (Math.abs(now - ts) > nonceStore.getTtlMillis()) {
            throw new CryptoException("요청이 만료되었거나 시각이 유효하지 않습니다.");
        }
        if (!nonceStore.tryConsume(plain.nonce(), now)) {
            throw new CryptoException("이미 처리된 요청입니다(재전송).");
        }
    }

    /** 프론트가 epoch 초/밀리 중 무엇을 보내도 밀리로 정규화(10자리=초). */
    private long normalizeToMillis(long timestamp) {
        return timestamp < 1_000_000_000_000L ? timestamp * 1000L : timestamp;
    }

    private String requireField(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new CryptoException(name + " 값이 없습니다.");
        }
        return value;
    }
}
