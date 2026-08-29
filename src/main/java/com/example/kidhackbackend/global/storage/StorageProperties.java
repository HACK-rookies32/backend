package com.example.kidhackbackend.global.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 첨부파일 저장소 설정.
 *
 * <p>{@code bucket} 이 비어 있으면 로컬 디스크 모드다. 운영에서는 WAS 부팅
 * 스크립트가 {@code APP_S3_BUCKET} 환경변수를 채워 넣는다
 * (relaxed binding: APP_S3_BUCKET -> app.s3.bucket).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.s3")
public class StorageProperties {

    /** S3 버킷 이름. 비어 있으면 로컬 디스크를 쓴다. */
    private String bucket = "";

    /** S3 키 접두사. 버킷을 다른 용도와 함께 쓰므로 첨부파일을 한 곳에 모은다. */
    private String prefix = "uploads";

    /**
     * 업로드용 presigned PUT URL 유효 기간.
     *
     * <p>짧을수록 URL 유출 시 노출 창이 좁다. 업로드는 발급 직후 바로 PUT 하는
     * 흐름이라 5 분이면 충분하다.
     */
    private Duration presignTtl = Duration.ofMinutes(5);

    /**
     * 다운로드용 presigned GET URL 유효 기간.
     *
     * <p>업로드보다 길게 둔다. {@code GET /api/files/...} 가 매 요청마다 302 로
     * 새 서명 URL 을 발급하므로, 브라우저가 리다이렉트 결과를 이 기간만큼 캐시하면
     * (Cache-Control) 이미지가 많은 화면에서 왕복과 서명 연산을 줄일 수 있다.
     * 캐시 유효기간은 이 값보다 짧게 잡아 서명이 캐시 도중 만료되지 않게 한다.
     */
    private Duration downloadTtl = Duration.ofHours(1);

    public boolean usesS3() {
        return StringUtils.hasText(bucket);
    }
}
