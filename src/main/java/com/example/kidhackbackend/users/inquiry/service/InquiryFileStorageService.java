package com.example.kidhackbackend.users.inquiry.service;

import com.example.kidhackbackend.global.storage.DbFileStorageService;
import com.example.kidhackbackend.global.storage.UploadCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 1:1 문의 첨부파일 업로드.
 *
 * <p>이전에는 S3/로컬 디스크({@code ObjectStorage})에 저장하고 DB 엔 경로 문자열만 담았다.
 * WAS 2 대 구성에서 로컬 저장이 인스턴스 간 공유되지 않는 문제 때문에, 첨부 실물을 이제
 * {@link DbFileStorageService} 를 통해 <b>DB(BLOB)</b> 에 담는다. 반환 경로 형태
 * ({@code /api/files/inquiry/{id}})는 조회 응답·프론트 표시 방식과 그대로 호환된다.
 *
 * <p>약한 필터·클라 Content-Type/원본 파일명 저장·로컬 미러 트래버설 쓰기 등 파일 업로드
 * 취약점은 {@link DbFileStorageService} 안에 그대로 있다([VULN-10] 계열).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryFileStorageService {

    private final DbFileStorageService dbFileStorageService;

    public String store(MultipartFile file) {
        // [VULN-10] 확장자 화이트리스트 검증 없음. 실제 싱크·저장은 DbFileStorageService 로 이동.
        //   재현 경로: FileController(/api/files/**) 도입 후 업로드 파일을 다시 받을 수 있어
        //   비로소 재현 가능해졌다. 조건부 판정 근거는 FileController 의 [VULN-10] 주석 참고.
        return dbFileStorageService.store(UploadCategory.INQUIRY, file);
    }

    /** 문의가 지워질 때 DB 에 담긴 첨부 실물도 함께 지운다. */
    public void delete(String publicPath) {
        DbFileStorageService.parseId(publicPath).ifPresent(id -> {
            try {
                dbFileStorageService.deleteById(id);
            } catch (RuntimeException e) {
                log.warn("문의 첨부 삭제 실패: {}", publicPath, e);
            }
        });
    }
}
