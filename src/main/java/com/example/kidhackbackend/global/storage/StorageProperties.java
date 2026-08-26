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
     * presigned URL 유효 기간.
     *
     * <p>짧을수록 URL 유출 시 노출 창이 좁다. 다만 사용자가 페이지를 열어 둔 채
     * 나중에 클릭하는 경우가 있으므로 지나치게 짧으면 만료된 링크를 보게 된다.
     * 매 요청마다 새로 발급되는 구조라 5 분이면 충분하다.
     */
    private Duration presignTtl = Duration.ofMinutes(5);

    public boolean usesS3() {
        return StringUtils.hasText(bucket);
    }
}
