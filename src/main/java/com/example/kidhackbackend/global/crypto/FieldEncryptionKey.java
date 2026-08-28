package com.example.kidhackbackend.global.crypto;

import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 저장 필드암호화(@Convert)용 AES-256 키 보관자.
 *
 * <p>키는 <b>하드코딩하지 않는다.</b> 설정({@code kidhack.card.storage-key}, Base64 32바이트)에서
 * 읽고, 비어 있으면(로컬) 기동 시 임시 키를 생성한다. 운영은 반드시 시크릿으로 주입해야 하며,
 * 그래야 재시작 후에도 기존 저장 데이터를 복호할 수 있다.
 */
@Component
public class FieldEncryptionKey {

    private static final int AES_256_BYTES = 32;

    private final byte[] key;

    public FieldEncryptionKey(@Value("${kidhack.card.storage-key:}") String configuredKey) {
        if (configuredKey != null && !configuredKey.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(configuredKey.trim());
            if (decoded.length != AES_256_BYTES) {
                throw new IllegalStateException(
                        "kidhack.card.storage-key 는 Base64 인코딩된 32바이트(AES-256) 키여야 합니다.");
            }
            this.key = decoded;
        } else {
            // 로컬 폴백: 임시 키 생성. H2 인메모리라 재시작 시 데이터도 함께 초기화되므로 무방.
            this.key = new byte[AES_256_BYTES];
            new SecureRandom().nextBytes(this.key);
        }
    }

    /**
     * 컨버터에 키를 심는다. {@link EncryptedStringConverter} 는 Hibernate 가 만들기 때문에
     * 주입을 받을 수 없어(그러면 슬라이스 테스트의 EMF 생성이 깨진다) 이쪽에서 밀어 넣는다.
     */
    @PostConstruct
    void bindToConverter() {
        EncryptedStringConverter.useKey(key);
    }
}
