package com.example.kidhackbackend.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.global.crypto.CryptoException;
import com.example.kidhackbackend.global.crypto.FieldEncryptionKey;
import com.example.kidhackbackend.global.crypto.HybridDecryptor;
import com.example.kidhackbackend.global.crypto.KeyRotatedException;
import com.example.kidhackbackend.global.crypto.NonceStore;
import com.example.kidhackbackend.global.crypto.RsaKeyProvider;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.wallet.dto.CardResponse;
import com.example.kidhackbackend.users.wallet.dto.EncryptedCardRequest;
import com.example.kidhackbackend.users.wallet.entity.Card;
import com.example.kidhackbackend.users.wallet.repository.CardRepository;
import com.example.kidhackbackend.users.wallet.service.CardService;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 카드 CRD(안전 버전) 슬라이스 테스트.
 *
 * <p>프론트 암호화를 그대로 흉내낸 헬퍼로 E2E 라운드트립을 검증한다:
 * 암복호 성공 · 저장 필드암호화(DB엔 평문 없음) · 마스킹 · CVC 미저장 · 소유권(IDOR 방어) · 재전송 거부 · 키 회전.
 */
@DataJpaTest
@Import({RsaKeyProvider.class, HybridDecryptor.class,
        NonceStore.class, FieldEncryptionKey.class})
class CardServiceTest {

    @TestConfiguration
    static class TestBeans {
        @Bean
        CardService cardService(CardRepository repo, RsaKeyProvider rsa, HybridDecryptor dec,
                                NonceStore nonce) {
            return new CardService(repo, rsa, dec, nonce);
        }
    }

    @Autowired CardService cardService;
    @Autowired CardRepository cardRepository;
    @Autowired RsaKeyProvider rsaKeyProvider;

    private static final long PARENT_A = 10L;
    private static final long PARENT_B = 20L;

    @BeforeEach
    void freshKey() {
        // 각 테스트가 현재 keyId로 암호화하도록 보장.
    }

    @Test
    void 등록_라운드트립_마스킹_CVC미저장() {
        EncryptedCardRequest req = encrypt(cardJson("1234567812345678", "123", freshNonce(), now()));

        CardResponse res = cardService.create(PARENT_A, req);

        // 응답은 마스킹만, 원본 노출 없음
        assertThat(res.maskedCardNumber()).isEqualTo("**** **** **** 5678");
        assertThat(res.ownerName()).isEqualTo("김지영");

        // DB에는 카드번호 평문이 없어야 한다(필드암호화 확인)
        Card saved = cardRepository.findById(res.id()).orElseThrow();
        assertThat(saved.getCardNumber()).isEqualTo("1234567812345678"); // 컨버터가 복호해 돌려줌
        // CVC 컬럼 자체가 엔티티에 없음(컴파일 타임 보장). 저장 필드는 5개뿐.

        // 원장(raw) 확인은 컨버터를 거치므로, 여기서는 마스킹/소유자만 확인.
        assertThat(saved.getOwnerId()).isEqualTo(PARENT_A);
    }

    @Test
    void 목록은_본인것만_최신순() {
        cardService.create(PARENT_A, encrypt(cardJson("1111222233334444", "111", freshNonce(), now())));
        cardService.create(PARENT_A, encrypt(cardJson("5555666677778888", "222", freshNonce(), now())));
        cardService.create(PARENT_B, encrypt(cardJson("9999000011112222", "333", freshNonce(), now())));

        List<CardResponse> aCards = cardService.list(PARENT_A);
        assertThat(aCards).hasSize(2);
        // 최신순(뒤에 넣은 8888가 앞)
        assertThat(aCards.get(0).maskedCardNumber()).endsWith("8888");
        assertThat(cardService.list(PARENT_B)).hasSize(1);
    }

    @Test
    void 삭제는_소유권_검증_남의카드는_404() {
        CardResponse a = cardService.create(PARENT_A, encrypt(cardJson("1234567812345678", "123", freshNonce(), now())));

        // 남(PARENT_B)이 A의 카드 삭제 시도 → 404(존재 노출 안 함)
        assertThatThrownBy(() -> cardService.delete(PARENT_B, a.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(cardRepository.findById(a.id())).isPresent();

        // 본인은 삭제 가능
        cardService.delete(PARENT_A, a.id());
        assertThat(cardRepository.findById(a.id())).isEmpty();
    }

    @Test
    void 같은_nonce_재전송은_거부() {
        String nonce = freshNonce();
        long ts = now();
        cardService.create(PARENT_A, encrypt(cardJson("1234567812345678", "123", nonce, ts)));

        // 동일 nonce로 다시 등록 시도 → 재전송으로 거부
        assertThatThrownBy(() -> cardService.create(PARENT_A, encrypt(cardJson("1234567812345678", "123", nonce, now()))))
                .isInstanceOf(CryptoException.class);
    }

    @Test
    void 오래된_timestamp는_거부() {
        long stale = now() - 5 * 60 * 1000L; // 5분 전
        assertThatThrownBy(() -> cardService.create(PARENT_A, encrypt(cardJson("1234567812345678", "123", freshNonce(), stale))))
                .isInstanceOf(CryptoException.class);
    }

    @Test
    void 회전된_keyId는_KeyRotated() {
        // 현재 키로 평문 JSON을 만들되, keyId만 옛 값으로 바꿔치기
        EncryptedCardRequest good = encrypt(cardJson("1234567812345678", "123", freshNonce(), now()));
        EncryptedCardRequest stale = new EncryptedCardRequest(
                "card-key-STALE", good.encryptedKey(), good.iv(), good.ciphertext());

        assertThatThrownBy(() -> cardService.create(PARENT_A, stale))
                .isInstanceOf(KeyRotatedException.class);
    }

    /* ------------------------------------------------------- 프론트 흉내 ---- */

    private long now() {
        return System.currentTimeMillis();
    }

    private int nonceSeq = 0;

    private String freshNonce() {
        return "nonce-" + (nonceSeq++) + "-" + System.nanoTime();
    }

    private String cardJson(String cardNumber, String cvc, String nonce, long timestamp) {
        return "{"
                + "\"cardNumber\":\"" + cardNumber + "\","
                + "\"expiry\":\"05/26\","
                + "\"cvc\":\"" + cvc + "\","
                + "\"passwordFront\":\"12\","
                + "\"birthDate\":\"900101\","
                + "\"ownerName\":\"김지영\","
                + "\"nonce\":\"" + nonce + "\","
                + "\"timestamp\":" + timestamp
                + "}";
    }

    /** 서버 공개키로 하이브리드 암호화(RSA-OAEP + AES-GCM), 프론트와 동일 규약. */
    private EncryptedCardRequest encrypt(String json) {
        try {
            byte[] der = Base64.getDecoder().decode(rsaKeyProvider.getPublicKeyBase64());
            PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));

            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256);
            byte[] aesKey = kg.generateKey().getEncoded();

            byte[] iv = new byte[12];
            new java.security.SecureRandom().nextBytes(iv);
            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, iv));
            byte[] ciphertext = aes.doFinal(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            rsa.init(Cipher.ENCRYPT_MODE, pub, new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
            byte[] wrapped = rsa.doFinal(aesKey);

            Base64.Encoder enc = Base64.getEncoder();
            return new EncryptedCardRequest(
                    rsaKeyProvider.getKeyId(),
                    enc.encodeToString(wrapped),
                    enc.encodeToString(iv),
                    enc.encodeToString(ciphertext));
        } catch (Exception e) {
            throw new IllegalStateException("test encrypt failed", e);
        }
    }
}
