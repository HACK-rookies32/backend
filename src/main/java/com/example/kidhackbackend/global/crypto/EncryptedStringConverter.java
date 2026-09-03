package com.example.kidhackbackend.global.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
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
        // 복호 실패 시 예외를 던지지 않고 null 을 돌려준다(카드 조회 500 방어).
        //
        // 복호 예외는 Hibernate 엔티티 로딩(ResultSet 매핑) 도중 여기서 터진다. 그대로 던지면
        // 리포지토리 조회 자체가 PersistenceException 으로 감싸져 나가고(전역 CryptoException 핸들러도
        // 못 잡는다), 복호 불가 카드가 하나라도 있으면 GET /api/wallet/cards 목록 전체가 500 이 됐다.
        // (운영에서 필드암호화 키가 재시작마다 바뀌어(kidhack.card.storage-key 미설정) 옛 카드가
        //  복호 불가가 된 사례.) null 을 돌려주면 그 필드만 비고(예: 카드번호 → 전체 마스킹),
        // 목록·삭제는 살아남는다. 근본 원인(운영 키 고정)은 별도 조치다.
        //
        // 쓰기(convertToDatabaseColumn)는 방어하지 않는다 — 저장은 암호화가 실패하면 반드시 막아야 한다.
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            return AesGcmCipher.utf8(AesGcmCipher.decryptWithPrefixedIv(requireKey(), decoded));
        } catch (IllegalArgumentException | CryptoException e) {
            // 어느 값이 실패했는지는 로그로 남기되(원본 평문·암호문은 남기지 않는다), 조회는 계속되게 한다.
            log.warn("암호화 필드 복호 실패 — null 로 대체한다. 저장 키가 바뀌었을 수 있다. cause={}", e.getMessage());
            return null;
        }
    }

    private static byte[] requireKey() {
        byte[] k = key;
        if (k == null) {
            throw new CryptoException("필드 암호화 키가 초기화되지 않았습니다.");
        }
        return k;
    }
}
