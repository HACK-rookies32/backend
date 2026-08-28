package com.example.kidhackbackend.global.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM 공용 유틸(무상태). 규약 확정본: IV 12바이트 랜덤, 인증태그 128비트.
 *
 * <p>두 곳에서 쓰인다.
 * <ul>
 *   <li>전송 복호: 프론트가 준 (ciphertext, iv)를 복호한 AES키로 푼다 — IV를 외부에서 받는다.</li>
 *   <li>저장 필드암호화: 저장 시 IV를 새로 만들어 {@code iv||ciphertext}로 합쳐 한 문자열로 보관한다.</li>
 * </ul>
 */
public final class AesGcmCipher {

    public static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    public static final int IV_LENGTH = 12;          // 96-bit IV (GCM 권장)
    public static final int TAG_LENGTH_BITS = 128;   // 128-bit 인증태그

    private static final SecureRandom RANDOM = new SecureRandom();

    private AesGcmCipher() {
    }

    /** 12바이트 랜덤 IV 생성. */
    public static byte[] randomIv() {
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        return iv;
    }

    /** 외부 IV로 복호(전송 payload 용). */
    public static byte[] decrypt(byte[] key, byte[] iv, byte[] ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new CryptoException("복호에 실패했습니다.", e);
        }
    }

    /** 새 IV로 암호화, {@code iv||ciphertext} 바이트로 반환(저장 필드암호화 용). */
    public static byte[] encryptWithPrefixedIv(byte[] key, byte[] plaintext) {
        try {
            byte[] iv = randomIv();
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext);
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return out;
        } catch (Exception e) {
            throw new CryptoException("암호화에 실패했습니다.", e);
        }
    }

    /** {@code iv||ciphertext} 바이트를 복호(저장 필드암호화 복원 용). */
    public static byte[] decryptWithPrefixedIv(byte[] key, byte[] ivAndCiphertext) {
        if (ivAndCiphertext.length <= IV_LENGTH) {
            throw new CryptoException("저장된 암호문 형식이 올바르지 않습니다.");
        }
        byte[] iv = new byte[IV_LENGTH];
        byte[] ct = new byte[ivAndCiphertext.length - IV_LENGTH];
        System.arraycopy(ivAndCiphertext, 0, iv, 0, IV_LENGTH);
        System.arraycopy(ivAndCiphertext, IV_LENGTH, ct, 0, ct.length);
        return decrypt(key, iv, ct);
    }

    public static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String utf8(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }
}
