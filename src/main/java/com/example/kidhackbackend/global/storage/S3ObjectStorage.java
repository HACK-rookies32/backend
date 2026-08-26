package com.example.kidhackbackend.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

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

            return new StoredObject(category, filename).publicPath();
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    /**
     * presigned PUT URL 발급.
     *
     * <p>{@link #store} 와 달리 파일 본문이 이 서버를 통과하지 않는다. WAS 2 대의
     * 대역폭과 요청 스레드를 아끼려는 것이 목적이다.
     *
     * <p>서명에 Content-Type 이 들어가므로 클라이언트는 반드시 같은 값을 헤더로
     * 보내야 한다. 브라우저에서 나가는 요청이라 preflight 가 발생하며, 버킷 CORS 에
     * PUT 이 열려 있어야 한다(infra storage.tf).
     */
    @Override
    public Optional<URI> uploadUrl(UploadCategory category, String filename, String contentType) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key(category, filename))
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
                .signatureDuration(props.getPresignTtl())
                .putObjectRequest(put)
                .build();

        return Optional.of(presigner.presignPutObject(presign).url())
                .map(url -> URI.create(url.toString()));
    }

    /**
     * HeadObject 로 존재만 본다. 본문을 받지 않으므로 크기와 무관하게 싸다.
     *
     * <p>SDK 버전에 따라 없는 키를 NoSuchKeyException 으로 주기도 하고 404 상태의
     * S3Exception 으로 주기도 해서 둘 다 받는다. 그 밖의 오류(권한 등)는 존재
     * 여부를 판단할 수 없는 상황이므로 false 로 삼키지 않고 그대로 올린다.
     */
    @Override
    public boolean exists(UploadCategory category, String filename) {
        try {
            client.headObject(HeadObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key(category, filename))
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    /**
     * 버킷에 버저닝이 켜져 있으므로 이 호출은 삭제 마커를 남기는 소프트 삭제다.
     * 이전 버전은 라이프사이클 규칙이 만료시킬 때까지 남아 복구할 수 있다.
     */
    @Override
    public void delete(UploadCategory category, String filename) {
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key(category, filename))
                .build());
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

    @Override
    public void destroy() {
        client.close();
        presigner.close();
    }
}
