package com.example.kidhackbackend.global.crypto;

import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 카드 E2E 전송 암호화용 RSA 키쌍 관리자.
 *
 * <p>프론트가 공개키로 1회용 AES 키를 암호화(RSA-OAEP)해 보내면, 여기 보관한 개인키로 복호한다.
 * 키에는 {@code keyId}를 부여해 회전(rotation)을 지원한다 — 프론트는 매 세션 공개키를 실시간
 * 조회하므로 회전해도 앱 재배포가 필요 없다(규약 확정본).
 *
 * <p><b>[FIX-CARD-KEY-SHARED]</b> 예전에는 기동 시 인스턴스마다 키쌍을 <b>메모리에만</b> 새로 만들었다.
 * 그러면 운영처럼 WAS 가 2 대이고 로드밸런서가 요청을 라운드로빈으로 뿌릴 때, 공개키 조회
 * ({@code GET /wallet/cards/public-key})와 카드 등록({@code POST /wallet/cards})이 서로 다른
 * 인스턴스로 가면 keyId 불일치(KEY_ROTATED)나 개인키 불일치(복호 실패)로 카드 등록이 깨진다.
 * 단일 인스턴스인 로컬(H2)에서는 드러나지 않아 통과했던 갭이다.
 *
 * <p>그래서 키쌍을 <b>Redis(ElastiCache)에 한 번만 만들어 전 인스턴스가 공유</b>한다. 먼저 저장된
 * 키를 읽고, 없으면 생성해 {@code SET NX}(원자적 최초쓰기)로 넣는다 — 동시에 여러 인스턴스가 떠도
 * 먼저 쓴 하나만 채택되고 나머지는 그 값을 읽어 같은 키를 쓴다. keyId 는 공개키 해시라 자동으로 같아진다.
 *
 * <p>Redis 가 없거나(로컬/CI, 슬라이스 테스트) 일시 장애면 기존처럼 <b>메모리 생성으로 폴백</b>한다
 * — 채팅 브로드캐스터가 Redis 장애 시 로컬 전달로 폴백하는 것과 같은 방어적 태도다. 카드 하나 때문에
 * 앱 전체 기동이 막히면 안 되기 때문이다(단일 인스턴스 로컬에서는 폴백이라도 문제없다).
 *
 * <p>개인키는 하드코딩하지 않는다. 공유 저장소(Redis)에 두거나(운영) 기동 시 생성해 메모리에만
 * 둔다(로컬). Redis 는 내부망 ElastiCache 라 외부 노출이 없다.
 */
@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    /** 하이브리드 규약: RSA-OAEP(SHA-256). */
    public static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int RSA_KEY_SIZE = 2048;

    /**
     * 공유 키쌍을 담는 Redis 키. 값은 {@code base64(PKCS8 개인키):base64(X.509 공개키)}.
     * 스키마/포맷이 바뀌면 접미사(v1)를 올려 옛 값과 충돌하지 않게 한다.
     */
    private static final String REDIS_KEYPAIR_KEY = "card:rsa:keypair:v1";

    private final long keyRotationDays;
    /** Redis 빈이 없을 수도 있으므로(로컬/CI/슬라이스 테스트) ObjectProvider 로 느슨하게 받는다. */
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private volatile String keyId;
    private volatile KeyPair keyPair;

    public RsaKeyProvider(@Value("${kidhack.card.key-rotation-days:90}") long keyRotationDays,
                          ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.keyRotationDays = keyRotationDays;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @PostConstruct
    void init() {
        loadOrCreateShared();
    }

    /**
     * 전 인스턴스가 공유하는 키쌍을 확보한다. Redis 에 있으면 읽고, 없으면 만들어 원자적으로 넣는다.
     * Redis 를 못 쓰면 메모리 생성으로 폴백한다.
     */
    synchronized void loadOrCreateShared() {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            // 로컬/CI/슬라이스 테스트 — Redis 빈 자체가 없다. 기존 동작(메모리 생성)과 동일.
            log.info("[card-key] Redis 미구성 — 인스턴스 로컬 키 생성(단일 인스턴스 전제)");
            rotate();
            return;
        }
        try {
            String stored = redis.opsForValue().get(REDIS_KEYPAIR_KEY);
            if (stored == null) {
                // 아직 아무도 안 만들었다 — 후보를 만들어 최초쓰기(NX)를 시도한다.
                KeyPair candidate = generateKeyPair();
                String encoded = encodeKeyPair(candidate);
                Boolean won = redis.opsForValue().setIfAbsent(REDIS_KEYPAIR_KEY, encoded);
                if (Boolean.TRUE.equals(won)) {
                    applyKeyPair(candidate);
                    log.info("[card-key] 공유 키쌍 신규 생성·저장 완료 (keyId={})", keyId);
                } else {
                    // 다른 인스턴스가 먼저 썼다 — 그 값을 읽어 동일 키를 쓴다.
                    stored = redis.opsForValue().get(REDIS_KEYPAIR_KEY);
                    applyKeyPair(decodeKeyPair(stored));
                    log.info("[card-key] 다른 인스턴스가 만든 공유 키쌍 채택 (keyId={})", keyId);
                }
            } else {
                applyKeyPair(decodeKeyPair(stored));
                log.info("[card-key] Redis 공유 키쌍 로드 (keyId={})", keyId);
            }
        } catch (Exception e) {
            // Redis 장애 등 — 앱 전체 기동을 막지 않는다. 메모리 생성으로 폴백(단일 인스턴스면 정상 동작).
            log.warn("[card-key] Redis 공유 키 로드 실패 — 메모리 생성으로 폴백", e);
            rotate();
        }
    }

    /**
     * 새 RSA 키쌍을 <b>메모리에</b> 생성하고 keyId를 갱신한다(공유 없음).
     * Redis 미구성/장애 시의 폴백 경로다. 옛 단일 인스턴스 동작과 동일해 로컬/CI 가 그대로 통과한다.
     */
    public synchronized void rotate() {
        applyKeyPair(generateKeyPair());
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_KEY_SIZE);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("RSA 키쌍 생성 실패", e);
        }
    }

    /** 키쌍을 적용하고 keyId 를 재계산한다. keyId 는 공개키 바이트 해시라 어느 인스턴스든 동일하다. */
    private void applyKeyPair(KeyPair pair) {
        this.keyPair = pair;
        // keyId는 공개키 해시 앞 부분 — 시간 함수 없이 결정적이고, 키가 바뀌면 달라진다.
        this.keyId = "card-key-" + Integer.toHexString(java.util.Arrays.hashCode(
                pair.getPublic().getEncoded()));
    }

    /** 키쌍을 {@code base64(PKCS8 개인키):base64(X.509 공개키)} 문자열로 직렬화한다. */
    private String encodeKeyPair(KeyPair pair) {
        String priv = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String pub = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        return priv + ":" + pub;
    }

    /** {@link #encodeKeyPair} 의 역변환. */
    private KeyPair decodeKeyPair(String encoded) {
        try {
            int sep = encoded.indexOf(':');
            byte[] privDer = Base64.getDecoder().decode(encoded.substring(0, sep));
            byte[] pubDer = Base64.getDecoder().decode(encoded.substring(sep + 1));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(privDer));
            PublicKey publicKey = factory.generatePublic(new X509EncodedKeySpec(pubDer));
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("공유 RSA 키쌍 복원 실패", e);
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
