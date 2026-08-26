package com.example.kidhackbackend.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * S3 저장소. 운영 경로다.
 *
 * <p>자격증명은 인스턴스 IAM 역할에서 온다. 컨테이너 안에서 IMDS 를 호출하므로
 * EC2 의 metadata hop limit 이 2 여야 한다(infra compute.tf 참고).
 */
@Slf4j
@RequiredArgsConstructor
public class S3ObjectStorage implements ObjectStorage, DisposableBean {

    private final S3Client client;
    private final S3Presigner presigner;
    private final StorageProperties props;

    @Override
    public String store(UploadCategory category, String filename, MultipartFile file) {
        String key = key(category, filename);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    // 클라이언트가 보낸 Content-Type 을 그대로 쓴다.
                    // 확장자 검증이 없는 것과 같은 맥락이며([VULN-10] 참고),
                    // 다운로드 시 브라우저가 이 값을 보고 렌더링 여부를 정한다.
                    .contentType(file.getContentType())
                    .build();

            client.putObject(request, RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));

            return publicPath(category, filename);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    @Override
    public Optional<URI> downloadUrl(UploadCategory category, String filename) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key(category, filename))
                .build();

        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(props.getPresignTtl())
                .getObjectRequest(get)
                .build();

        return Optional.of(presigner.presignGetObject(presign).url().toString())
                .map(URI::create);
    }

    /**
     * S3 모드에서는 호출되지 않는다. {@link #downloadUrl} 이 항상 값을 돌려주므로
     * 컨트롤러가 리다이렉트로 끝낸다. 인터페이스 계약상 남겨 둔다.
     */
    @Override
    public Resource loadAsResource(UploadCategory category, String filename) {
        throw new UnsupportedOperationException(
                "S3 모드에서는 presigned URL 리다이렉트를 쓴다. downloadUrl 을 먼저 확인할 것.");
    }

    private String key(UploadCategory category, String filename) {
        return "%s/%s/%s".formatted(props.getPrefix(), category.segment(), filename);
    }

    private String publicPath(UploadCategory category, String filename) {
        return "/api/files/%s/%s".formatted(category.segment(), filename);
    }

    @Override
    public void destroy() {
        client.close();
        presigner.close();
    }
}
