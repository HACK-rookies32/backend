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
 *
 * <p><b>업로드 경로가 둘이다.</b>
 * <ul>
 *   <li>{@link #store} — 브라우저가 WAS 로 multipart 를 보내고 WAS 가 S3 에 올린다.
 *       작은 파일용이며 로컬 모드에서도 그대로 동작한다.</li>
 *   <li>{@link #uploadUrl} — presigned PUT URL 만 발급하고 파일 본문은 브라우저가
 *       S3 로 직접 보낸다. WAS 를 통과하지 않으므로 큰 파일에 쓴다.</li>
 * </ul>
 */
public interface ObjectStorage {

    /**
     * 파일을 저장하고 공개 경로를 반환한다.
     *
     * @return {@code /api/files/{category}/{filename}} 형태. 이 문자열이 DB 에 저장된다.
     */
    String store(UploadCategory category, String filename, MultipartFile file);

    /**
     * 브라우저가 S3 로 직접 올릴 presigned PUT URL.
     *
     * <p>서명에 Content-Type 이 포함되므로 클라이언트는 PUT 할 때 여기 넘긴 값과
     * <b>똑같은</b> Content-Type 헤더를 보내야 한다. 다르면 S3 가 403 을 준다.
     *
     * <p>로컬 구현은 서명할 대상이 없어 비어 있는 값을 돌려준다. 그 경우 호출하는
     * 쪽은 {@link #store} 를 쓰는 기존 multipart 경로로 안내해야 한다.
     */
    Optional<URI> uploadUrl(UploadCategory category, String filename, String contentType);

    /**
     * 객체가 실제로 있는지 본다.
     *
     * <p>presigned 업로드는 WAS 를 거치지 않으므로 서버는 업로드 성공 여부를 모른다.
     * 클라이언트가 "다 올렸다" 고 알려 온 것을 그대로 믿고 DB 에 경로를 박으면
     * 실재하지 않는 파일을 가리키게 되므로, 완료 시점에 이걸로 확인한다.
     */
    boolean exists(UploadCategory category, String filename);

    /**
     * 객체를 지운다. 없는 객체를 지워도 예외를 던지지 않는다(멱등).
     *
     * <p>운영 버킷은 버저닝이 켜져 있어 이 호출은 삭제 마커를 남길 뿐이고 실제
     * 데이터는 라이프사이클 규칙이 만료시킬 때까지 복구할 수 있다(infra storage.tf).
     */
    void delete(UploadCategory category, String filename);

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
