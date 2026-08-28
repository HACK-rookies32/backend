package com.example.kidhackbackend.global.crypto;

import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 카드 E2E 전송 암호화용 RSA 키쌍 관리자.
 *
 * <p>프론트가 공개키로 1회용 AES 키를 암호화(RSA-OAEP)해 보내면, 여기 보관한 개인키로 복호한다.
 * 키에는 {@code keyId}를 부여해 회전(rotation)을 지원한다 — 프론트는 매 세션 공개키를 실시간
 * 조회하므로 회전해도 앱 재배포가 필요 없다(규약 확정본).
 *
 * <p>개인키는 <b>하드코딩하지 않는다.</b> 기동 시 새로 생성해 메모리에만 둔다. 로컬(H2)은
 * 재시작마다 데이터가 초기화되므로 키가 매번 바뀌어도 무방하고, 운영은 인프라가 프로세스 수명 동안
 * 유지되며 keyId 실시간 조회로 회전을 흡수한다.
 */
@Component
public class RsaKeyProvider {

    /** 하이브리드 규약: RSA-OAEP(SHA-256). */
    public static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int RSA_KEY_SIZE = 2048;

    private final long keyRotationDays;

    private volatile String keyId;
    private volatile KeyPair keyPair;

    public RsaKeyProvider(@Value("${kidhack.card.key-rotation-days:90}") long keyRotationDays) {
        this.keyRotationDays = keyRotationDays;
    }

    @PostConstruct
    void init() {
        rotate();
    }

    /** 새 RSA 키쌍을 생성하고 keyId를 갱신한다. */
    public synchronized void rotate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_KEY_SIZE);
            this.keyPair = generator.generateKeyPair();
            // keyId는 공개키 해시 앞 12자리 — 시간 함수 없이 결정적이고, 회전마다 달라진다.
            this.keyId = "card-key-" + Integer.toHexString(java.util.Arrays.hashCode(
                    this.keyPair.getPublic().getEncoded()));
        } catch (Exception e) {
            throw new IllegalStateException("RSA 키쌍 생성 실패", e);
        }
    }

    /** 현재 유효한 keyId. */
    public String getKeyId() {
        return keyId;
    }

    /** 현재 공개키(Base64, X.509 DER). 프론트가 RSA-OAEP 암호화에 사용한다. */
    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    /** 회전 주기(일) — 공개키 응답에 참고값으로 노출한다. */
    public long getKeyRotationDays() {
        return keyRotationDays;
    }

    PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    /** 요청 keyId가 현재 유효한 키인지. 불일치면 회전된 것으로 보고 재조회를 유도한다. */
    public boolean isCurrentKey(String requestKeyId) {
        return keyId != null && keyId.equals(requestKeyId);
    }
}
