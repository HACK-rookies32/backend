package com.example.kidhackbackend.users.communityboard.service;

import com.example.kidhackbackend.global.storage.ObjectStorage;
import com.example.kidhackbackend.global.storage.StoredObject;
import com.example.kidhackbackend.global.storage.UploadCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 커뮤니티 첨부파일 업로드.
 *
 * <p>실제 저장은 {@link ObjectStorage} 가 한다. 운영에서는 S3, 로컬에서는 디스크이며
 * {@code app.s3.bucket} 설정 여부로 갈린다.
 *
 * <p>파일명 생성만 여기 남긴 이유는 저장 위치와 무관한 결정이기 때문이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final ObjectStorage storage;

    public String store(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        // [VULN-10] 확장자 화이트리스트 검증이 없어서 어떤 파일이든 그대로 저장됨
        //
        // 참고: 이전에는 올린 파일을 다시 받을 경로가 없어 이 취약점이 사실상
        // 잠들어 있었다. FileController(/api/files/**) 가 생기면서 실제로 재현
        // 가능해졌다. Content-Type 도 클라이언트가 보낸 값을 그대로 쓰므로
        // 임의 파일에 text/html 을 붙여 브라우저에 렌더링시킬 수 있다.
        //
        // 다만 presigned URL 이 S3 오리진을 가리키므로 서비스 도메인의 쿠키나
        // localStorage(JWT)에는 접근할 수 없다. 피해 범위가 오리진 격리로 제한된다.
        String savedFilename = UUID.randomUUID() + "_" + originalFilename;

        return storage.store(UploadCategory.COMMUNITY, savedFilename, file);
    }

    /**
     * 게시글이 지워질 때 첨부 실물도 함께 지운다.
     *
     * <p>이게 없으면 게시글만 사라지고 S3 객체는 영원히 남는다(고아 객체).
     *
     * <p>어떤 실패도 게시글 삭제를 막지 않는다. 첨부를 못 지운 것은 로그로 남겨
     * 나중에 정리할 수 있지만, 그것 때문에 사용자가 글을 못 지우면 안 된다.
     *
     * <p>커뮤니티 분류가 아닌 경로와 우리가 만들지 않은 값(외부 URL, 예전 데이터)은
     * 조용히 건너뛴다. 분류를 대조하는 이유는 게시글 첨부 목록에 문의 첨부 경로를
     * 넣어 남의 파일을 지우는 것을 막기 위해서다.
     */
    public void delete(String publicPath) {
        StoredObject.parse(publicPath)
                .filter(object -> object.category() == UploadCategory.COMMUNITY)
                .ifPresent(object -> {
                    try {
                        storage.delete(object.category(), object.filename());
                    } catch (RuntimeException e) {
                        log.warn("첨부파일 삭제 실패: {}", publicPath, e);
                    }
                });
    }
}
