package com.example.kidhackbackend.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class FirebaseConfig {

    @Value("${firebase.credentials-path}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(openCredentials()))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase 초기화 성공 ({})", credentialsPath);
            }
        } catch (IOException e) {
            log.warn("Firebase 초기화 실패 - 서비스 계정 키 파일이 없거나 잘못됨. 푸시 알림 기능은 비활성화됩니다. ({})", e.getMessage());
        }
    }

    /**
     * 절대 경로면 파일시스템에서, 아니면 클래스패스에서 읽는다.
     *
     * <p>컨테이너로 배포하면서 필요해진 분기다. 클래스패스는 곧 jar 내부이므로
     * 서비스 계정 키를 볼륨으로 넣어줄 방법이 없고, 그렇다고 이미지에 구우면
     * Docker Hub 에 개인키가 박힌다(프라이빗 레포라도 레지스트리 자격증명이
     * 새면 키도 같이 샌다).
     *
     * <p>운영에서는 환경변수로 덮어쓴다:
     * {@code FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase.json}
     * (relaxed binding 으로 firebase.credentials-path 에 매핑된다)
     *
     * <p>로컬은 application.yml 기본값이 상대 경로라 기존 동작 그대로다.
     */
    private InputStream openCredentials() throws IOException {
        Resource resource = credentialsPath.startsWith("/")
                ? new FileSystemResource(credentialsPath)
                : new ClassPathResource(credentialsPath);
        return resource.getInputStream();
    }
}