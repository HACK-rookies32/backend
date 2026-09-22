package com.example.kidhackbackend.global.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * 문의 첨부·프로필 이미지를 DB(BLOB)에 담고 다시 내려주는 저장소.
 *
 * <p>저장 자체는 DB 라 두 WAS 가 공유해 다운로드가 안정적이지만, 파일 업로드 취약점은
 * 코드에 그대로 심어 둔다:
 * <ol>
 *   <li>{@link UploadFilter} — 막는 척하지만 우회 가능한 약한 필터.</li>
 *   <li>Content-Type·원본 파일명을 클라이언트 값 그대로 저장([VULN-10]).</li>
 *   <li>DB 저장과 별개로 <b>로컬 디스크에도 원본 파일명 그대로</b> 쓴다 — 파일명에
 *       {@code ../} 를 넣으면 {@code uploads/} 밖으로 임의 경로 쓰기(트래버설 쓰기).
 *       cron 등 "쓴 파일을 실행해 주는 싱크"가 있으면 RCE 체인의 1단계가 된다.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbFileStorageService {

    private final DbFileRepository repository;
    private final UploadFilter uploadFilter;
    private final StorageProperties props;

    /** 이 서비스가 다루는(= DB BLOB) 분류. 나머지 분류는 기존 ObjectStorage(S3/로컬) 그대로. */
    public static boolean handles(UploadCategory category) {
        return category == UploadCategory.INQUIRY || category == UploadCategory.PROFILE
                || category == UploadCategory.CHAT;
    }

    /**
     * 업로드 → DB 저장 후 공개 경로 {@code /api/files/{seg}/{id}} 반환.
     */
    @Transactional
    public String store(UploadCategory category, MultipartFile file) {
        uploadFilter.validate(file); // 우회 가능한 약한 필터

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("파일을 읽을 수 없습니다.", e);
        }

        DbFile saved = repository.save(new DbFile(
                category.segment(),
                bytes,
                file.getContentType(),          // [VULN-10] 클라 Content-Type 그대로
                file.getOriginalFilename(),      // [VULN-10] 원본 파일명 그대로
                file.getSize()));

        // [VULN] DB 저장과 별개로 로컬 FS 에도 무새니타이즈로 쓴다 → 트래버설 쓰기(임의 경로).
        //   기능상 불필요한 중복 쓰기지만, 이게 파일 업로드 취약점(경로 조작 쓰기 → RCE 체인)의 진입점이다.
        try {
            Path target = Paths.get(props.getPrefix(), category.segment(), file.getOriginalFilename());
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.write(target, bytes);
        } catch (Exception ignore) {
            // best-effort — 취약점 재현용이라 실패해도 업로드는 성공 처리한다.
            log.debug("local mirror write skipped: {}", file.getOriginalFilename());
        }

        return "/api/files/" + category.segment() + "/" + saved.getId();
    }

    public Optional<DbFile> loadById(Long id) {
        return repository.findById(id);
    }

    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    /** {@code /api/files/{seg}/{id}} 형태 경로에서 DbFile id 를 뽑는다. 아니면 empty. */
    public static Optional<Long> parseId(String publicPath) {
        if (publicPath == null) {
            return Optional.empty();
        }
        int lastSlash = publicPath.lastIndexOf('/');
        if (lastSlash < 0) {
            return Optional.empty();
        }
        String tail = publicPath.substring(lastSlash + 1);
        try {
            return Optional.of(Long.parseLong(tail));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
