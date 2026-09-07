package com.example.kidhackbackend.users.inquiry.service;

import com.example.kidhackbackend.global.storage.ObjectStorage;
import com.example.kidhackbackend.global.storage.StoredObject;
import com.example.kidhackbackend.global.storage.UploadCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 1:1 문의 첨부파일 업로드.
 *
 * <p>구조는 커뮤니티 쪽 FileStorageService 와 같고 저장 분류만 다르다.
 * 실제 저장은 {@link ObjectStorage} 가 하며, 운영에서는 S3, 로컬에서는 디스크다
 * ({@code app.s3.bucket} 설정 여부로 갈린다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryFileStorageService {

    private final ObjectStorage storage;

    public String store(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        // [VULN-10] 확장자 화이트리스트 검증이 없어 어떤 파일이든 그대로 저장된다.
        // 커뮤니티 첨부와 완전히 같은 결함이며, 문의 첨부 카테고리에도 그대로 적용된다.
        //
        // 참고: 이전에는 올린 파일을 다시 받을 경로가 없어 이 취약점이 사실상 잠들어 있었다.
        // FileController(/api/files/**) 가 생기면서 실제로 재현 가능해졌다.
        // 실증 결과와 '조건부' 판정 근거(S3 출처 분리)는 FileController 의 [VULN-10] 주석 참고.
        String savedFilename = UUID.randomUUID() + "_" + originalFilename;

        return storage.store(UploadCategory.INQUIRY, savedFilename, file);
    }

    /**
     * 문의가 지워질 때 첨부 실물도 함께 지운다.
     *
     * <p>구조와 실패 정책은 커뮤니티 쪽 FileStorageService#delete 주석 참고.
     * 문의 첨부에는 개인정보가 담기는 경우가 있어 고아 객체를 남기지 않는 것이
     * 커뮤니티보다 더 중요하다.
     */
    public void delete(String publicPath) {
        StoredObject.parse(publicPath)
                .filter(object -> object.category() == UploadCategory.INQUIRY)
                .ifPresent(object -> {
                    try {
                        storage.delete(object.category(), object.filename());
                    } catch (RuntimeException e) {
                        log.warn("문의 첨부파일 삭제 실패: {}", publicPath, e);
                    }
                });
    }
}
