package com.example.kidhackbackend.global.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Base64;

/**
 * 엔티티 문자열 필드를 <b>저장 시 AES-256-GCM으로 암호화</b>하고 조회 시 복호하는 JPA 컨버터.
 * 카드번호·비번앞2·생년월일·소유자명에 {@code @Convert(converter = EncryptedStringConverter.class)}로 적용한다.
 *
 * <p>⚠️ <b>이 클래스는 스프링 빈에 의존해서는 안 된다.</b> Hibernate 는 컨버터를
 * {@code SpringBeanContainer} 를 통해 만드는데, 빈으로 등록되어 있지 않으면 그 자리에서
 * 생성자 주입을 시도한다. 그래서 생성자에 빈을 받으면 컴포넌트 스캔이 없는
 * {@code @DataJpaTest} 슬라이스에서 <b>EntityManagerFactory 생성 자체가 실패</b>하고,
 * Card 엔티티와 무관한 슬라이스 테스트까지 전부 죽는다(실제로 겪은 문제).
 *
 * <p>그래서 키는 static 홀더로 받는다 — {@link FieldEncryptionKey} 빈이 기동 시
 * {@link #useKey(byte[])} 로 심어 주고, Hibernate 가 만드는 인스턴스도 같은 키를 공유한다.
 * 키가 심어지지 않은 채 변환이 일어나면 조용히 넘어가지 않고 예외를 던진다.
 *
 * <p>DB에는 {@code Base64(iv || ciphertext)} 문자열로 저장된다.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static volatile byte[] key;

    /** {@link FieldEncryptionKey} 가 기동 시 호출한다. */
    static void useKey(byte[] encryptionKey) {
        key = encryptionKey;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        byte[] encrypted = AesGcmCipher.encryptWithPrefixedIv(requireKey(), AesGcmCipher.utf8(attribute));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        byte[] decoded = Base64.getDecoder().decode(dbData);
        return AesGcmCipher.utf8(AesGcmCipher.decryptWithPrefixedIv(requireKey(), decoded));
    }

    private static byte[] requireKey() {
        byte[] k = key;
        if (k == null) {
            throw new CryptoException("필드 암호화 키가 초기화되지 않았습니다.");
        }
        return k;
    }
}
