package com.example.kidhackbackend.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * 로컬 디스크 저장소. {@code app.s3.bucket} 이 비어 있을 때 쓰인다.
 *
 * <p>기존 동작을 그대로 옮긴 것이라 로컬 개발 흐름이 바뀌지 않는다.
 * 저장 위치도 예전과 같은 {@code uploads/community}, {@code uploads/inquiry} 다.
 *
 * <p>운영에서는 쓰이지 않는다. WAS 가 2 대라 한쪽에 올린 파일을 다른 쪽이
 * 서빙하지 못하기 때문이다.
 */
@RequiredArgsConstructor
public class LocalObjectStorage implements ObjectStorage {

    private final StorageProperties props;

    @Override
    public String store(UploadCategory category, String filename, MultipartFile file) {
        try {
            Path dir = dir(category);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            // transferTo 는 상대 경로를 임시 디렉터리 기준으로 해석하는 구현이 있어
            // 절대 경로로 넘긴다.
            file.transferTo(dir.resolve(filename).toAbsolutePath());
            return new StoredObject(category, filename).publicPath();
        } catch (IOException e) {
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    /**
     * 로컬에는 서명할 대상이 없다. 비어 있으면 컨트롤러가 기존 multipart 업로드를
     * 쓰라고 안내한다. 로컬 개발자가 AWS 자격증명 없이도 계속 작업할 수 있어야 하므로
     * 예외를 던지지 않는다.
     */
    @Override
    public Optional<URI> uploadUrl(UploadCategory category, String filename, String contentType) {
        return Optional.empty();
    }

    @Override
    public boolean exists(UploadCategory category, String filename) {
        return Files.isRegularFile(dir(category).resolve(filename));
    }

    @Override
    public void delete(UploadCategory category, String filename) {
        try {
            Files.deleteIfExists(dir(category).resolve(filename));
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제에 실패했습니다.", e);
        }
    }

    /** 로컬에는 서명할 대상이 없다. 비어 있으면 컨트롤러가 직접 스트리밍한다. */
    @Override
    public Optional<URI> downloadUrl(UploadCategory category, String filename) {
        return Optional.empty();
    }

    @Override
    public Resource loadAsResource(UploadCategory category, String filename) {
        return new FileSystemResource(dir(category).resolve(filename));
    }

    private Path dir(UploadCategory category) {
        return Paths.get(props.getPrefix(), category.segment());
    }
}
