package com.example.kidhackbackend.global.crypto;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.spec.MGF1ParameterSpec;
import org.springframework.stereotype.Component;

/**
 * 하이브리드 E2E 복호기(RSA-OAEP + AES-GCM). 규약 확정본 그대로.
 *
 * <pre>
 * 1) keyId가 현재 서버 키인지 확인 (아니면 KeyRotatedException → 프론트 공개키 재조회)
 * 2) RSA-OAEP(SHA-256) 개인키로 encryptedKey 복호 → 1회용 AES-256 키
 * 3) 그 AES키로 (iv, ciphertext) AES-GCM 복호 → 평문 JSON
 * </pre>
 *
 * <p>카드 도메인에 종속되지 않는다 — 평문 문자열(JSON)을 돌려줄 뿐, 스키마 해석·재전송 검증은 호출자가 한다.
 */
@Component
public class HybridDecryptor {

    private final RsaKeyProvider rsaKeyProvider;

    public HybridDecryptor(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    /**
     * @param keyId 요청이 사용한 공개키 식별자
     * @param encryptedKeyBase64 RSA-OAEP(AES키) Base64
     * @param ivBase64 AES-GCM IV Base64 (12바이트)
     * @param ciphertextBase64 AES-GCM(JSON) Base64
     * @return 복호된 평문(JSON 문자열)
     */
    public String decrypt(String keyId, String encryptedKeyBase64, String ivBase64, String ciphertextBase64) {
        if (!rsaKeyProvider.isCurrentKey(keyId)) {
            throw new KeyRotatedException("공개키가 회전되었습니다. 최신 공개키로 다시 시도하세요.");
        }
        byte[] aesKey = rsaUnwrap(decodeBase64(encryptedKeyBase64, "encryptedKey"));
        byte[] iv = decodeBase64(ivBase64, "iv");
        byte[] ciphertext = decodeBase64(ciphertextBase64, "ciphertext");
        if (iv.length != AesGcmCipher.IV_LENGTH) {
            throw new CryptoException("IV 길이가 올바르지 않습니다.");
        }
        byte[] plaintext = AesGcmCipher.decrypt(aesKey, iv, ciphertext);
        return AesGcmCipher.utf8(plaintext);
    }

    /** RSA-OAEP(SHA-256)로 감싼 AES키를 개인키로 푼다. */
    private byte[] rsaUnwrap(byte[] wrapped) {
        try {
            Cipher cipher = Cipher.getInstance(RsaKeyProvider.RSA_TRANSFORMATION);
            OAEPParameterSpec oaep = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, rsaKeyProvider.getPrivateKey(), oaep);
            return cipher.doFinal(wrapped);
        } catch (Exception e) {
            throw new CryptoException("키 복호에 실패했습니다.", e);
        }
    }

    private byte[] decodeBase64(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new CryptoException(field + " 값이 비어 있습니다.");
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new CryptoException(field + " Base64 형식이 올바르지 않습니다.");
        }
    }
}
