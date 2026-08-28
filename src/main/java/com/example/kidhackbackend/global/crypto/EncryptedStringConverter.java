package com.example.kidhackbackend.global.crypto;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 엔티티 문자열 필드를 <b>저장 시 AES-256-GCM으로 암호화</b>하고 조회 시 복호하는 JPA 컨버터.
 * 카드번호·비번앞2·생년월일·소유자명에 {@code @Convert(converter = EncryptedStringConverter.class)}로 적용한다.
 *
 * <p>JPA 컨버터는 Hibernate가 생성하므로 Spring 의존성 주입이 보장되지 않는다. 그래서 키는
 * static 홀더로 전달한다 — 이 클래스에 {@code @Component}를 달아 스프링이 한 번 인스턴스화할 때
 * {@link FieldEncryptionKey}를 주입받아 static 필드에 심는다(@PostConstruct). Hibernate가 만드는
 * 인스턴스도 같은 static 키를 공유한다.
 *
 * <p>DB에는 {@code Base64(iv || ciphertext)} 문자열로 저장된다.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static volatile byte[] key;

    private final FieldEncryptionKey fieldEncryptionKey;

    public EncryptedStringConverter(FieldEncryptionKey fieldEncryptionKey) {
        this.fieldEncryptionKey = fieldEncryptionKey;
    }

    @PostConstruct
    void bind() {
        key = fieldEncryptionKey.raw();
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
