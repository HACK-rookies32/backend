package com.example.kidhackbackend.global.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Optional;

/**
 * 첨부파일 저장소.
 *
 * <p>구현이 둘이다. {@code app.s3.bucket} 이 설정되어 있으면 {@link S3ObjectStorage},
 * 비어 있으면 {@link LocalObjectStorage} 가 주입된다. 로컬 폴백을 남겨 두는 이유는
 * 백엔드 개발자가 업로드를 테스트할 때마다 AWS 자격증명을 갖춰야 하는 상황을
 * 피하기 위해서다. 운영에서는 WAS 부팅 스크립트가 APP_S3_BUCKET 을 채운다.
 *
 * <p>파일명 생성은 이 인터페이스의 책임이 아니다. 호출하는 서비스가 정한 이름을
 * 그대로 받는다.
 */
public interface ObjectStorage {

    /**
     * 파일을 저장하고 공개 경로를 반환한다.
     *
     * @return {@code /api/files/{category}/{filename}} 형태. 이 문자열이 DB 에 저장된다.
     */
    String store(UploadCategory category, String filename, MultipartFile file);

    /**
     * 브라우저를 리다이렉트시킬 다운로드 URL.
     *
     * <p>S3 구현은 presigned URL 을 만들어 돌려주고, 로컬 구현은 비어 있는 값을
     * 돌려준다. 비어 있으면 컨트롤러가 {@link #loadAsResource} 로 직접 스트리밍한다.
     *
     * <p>presigned URL 을 DB 에 저장하지 않고 매번 발급하는 이유는 만료되기 때문이다.
     * DB 에는 위 {@link #store} 가 돌려준 경로만 남는다.
     */
    Optional<URI> downloadUrl(UploadCategory category, String filename);

    /** 리다이렉트를 쓸 수 없을 때 파일 자체를 읽는다. */
    Resource loadAsResource(UploadCategory category, String filename);
}
