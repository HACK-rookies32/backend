package com.example.kidhackbackend.global.notification.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link PushPort} 구현체를 고른다.
 *
 * <p>예전에는 이 판단이 세 곳(FirebaseConfig, FirebasePushSender, MockPushSender)의
 * {@code @ConditionalOnProperty} 에 흩어져 있어 "언제 무엇이 뜨는지" 가 보이지 않았다.
 * 게다가 Firebase 초기화 실패를 warn 으로 삼켜, 운영에서 푸시가 조용히 죽어도 아무도
 * 몰랐다. 결정과 실패 처리를 여기 한 곳에 모은다.
 *
 * <p><b>키를 못 읽었을 때</b>
 * <ul>
 *   <li>운영(prod) : 기동을 실패시킨다. 푸시가 조용히 죽은 채 배포가 끝나는 것보다
 *       컨테이너가 안 뜨는 편이 훨씬 빨리 발견된다.</li>
 *   <li>그 외 : 경고를 남기고 mock 으로 폴백한다. 팀원 전원이 서비스 계정 키를 들고
 *       있어야 로컬 개발이 되는 상황을 만들지 않는다.</li>
 * </ul>
 */
@Slf4j
@Configuration
public class PushConfig {

    private static final String FIREBASE = "firebase";

    @Bean
    public PushPort pushPort(
            Environment environment,
            ResourceLoader resourceLoader,
            @Value("${app.push.provider:mock}") String provider,
            @Value("${firebase.credentials-path:}") String credentialsPath) {

        if (!FIREBASE.equalsIgnoreCase(provider)) {
            log.info("푸시 제공자: {} — 실제 발송 없이 로그만 남긴다.", provider);
            return new MockPushSender();
        }

        try {
            FirebaseApp app = firebaseApp(resourceLoader, credentialsPath);
            log.info("푸시 제공자: firebase (키={})", credentialsPath);
            return new FirebasePushSender(FirebaseMessaging.getInstance(app));
        } catch (Exception e) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException(
                        "운영에서 Firebase 초기화에 실패했다. firebase.credentials-path("
                                + credentialsPath + ") 에 서비스 계정 키가 마운트됐는지 확인할 것.", e);
            }
            log.warn("Firebase 초기화 실패 — mock 으로 폴백한다. 푸시는 로그로만 남는다. ({})",
                    e.getMessage());
            return new MockPushSender();
        }
    }

    /**
     * 서비스 계정 키를 읽어 {@link FirebaseApp} 을 만든다.
     *
     * <p>{@link ResourceLoader} 를 쓰는 이유: 운영은 SSM 에서 받은 키를 컨테이너에
     * {@code /run/secrets/firebase.json} 으로 <b>마운트</b>하므로 클래스패스가 아니다.
     * 예전 {@code ClassPathResource} 는 jar 내부만 뒤져서 이 경로를 영영 못 찾았다.
     * 경로 앞에 {@code classpath:} / {@code file:} 을 붙여 구분한다.
     */
    private FirebaseApp firebaseApp(ResourceLoader resourceLoader, String credentialsPath)
            throws IOException {

        if (!StringUtils.hasText(credentialsPath)) {
            throw new IllegalStateException("firebase.credentials-path 가 비어 있다.");
        }

        // 컨텍스트가 여러 번 뜨는 테스트에서 재초기화(IllegalStateException)를 피한다.
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        Resource resource = resourceLoader.getResource(credentialsPath);
        try (InputStream keyStream = resource.getInputStream()) {
            return FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(keyStream))
                    .build());
        }
    }
}