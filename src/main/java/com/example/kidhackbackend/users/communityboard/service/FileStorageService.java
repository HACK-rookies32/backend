package com.example.kidhackbackend.users.communityboard.service;

import com.example.kidhackbackend.global.storage.ObjectStorage;
import com.example.kidhackbackend.global.storage.UploadCategory;
import lombok.RequiredArgsConstructor;
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
}
