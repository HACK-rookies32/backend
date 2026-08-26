package com.example.kidhackbackend.global.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * 저장소 구현 선택.
 *
 * <p>{@code @ConditionalOnProperty} 대신 명시적 분기를 쓴다. 빈 문자열을 조건으로
 * 다루기가 까다롭고, 무엇보다 S3Client 생성 자체가 리전을 즉시 해석하며 실패할 수
 * 있어 로컬 모드에서는 아예 만들지 않아야 하기 때문이다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public ObjectStorage objectStorage(StorageProperties props) {
        if (!props.usesS3()) {
            log.info("첨부파일 저장소: 로컬 디스크 (app.s3.bucket 미설정)");
            return new LocalObjectStorage(props);
        }

        // 리전은 AWS_REGION 환경변수 -> 프로파일 -> IMDS 순으로 해석된다.
        // WAS 부팅 스크립트가 AWS_REGION 을 넣어 주므로 IMDS 왕복이 생기지 않는다.
        // 자격증명은 인스턴스 IAM 역할에서 온다.
        log.info("첨부파일 저장소: S3 (bucket={}, prefix={})", props.getBucket(), props.getPrefix());
        return new S3ObjectStorage(S3Client.create(), S3Presigner.create(), props);
    }
}
