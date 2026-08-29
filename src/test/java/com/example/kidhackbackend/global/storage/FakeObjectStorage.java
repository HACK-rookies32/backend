package com.example.kidhackbackend.global.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 테스트용 {@link ObjectStorage}. 슬라이스 테스트에서 {@link AttachmentValidator} 를 함께
 * 올려야 할 때 쓴다.
 *
 * <p>기본은 <b>모든 파일이 존재</b>(존재 확인이 첨부 검증을 통과)로 둔다. 없음을 검증하려면
 * {@link #markMissing()} 을 호출한다.
 */
public class FakeObjectStorage implements ObjectStorage {

    private final Set<String> stored = new HashSet<>();
    private boolean everythingExists = true;

    /** 이후 exists() 는 실제로 store/putExisting 한 키에 대해서만 true. */
    public void markMissing() {
        this.everythingExists = false;
    }

    public void putExisting(UploadCategory category, String filename) {
        stored.add(key(category, filename));
    }

    @Override
    public String store(UploadCategory category, String filename, MultipartFile file) {
        stored.add(key(category, filename));
        return new StoredObject(category, filename).publicPath();
    }

    @Override
    public Optional<URI> uploadUrl(UploadCategory category, String filename, String contentType) {
        return Optional.empty();
    }

    @Override
    public boolean exists(UploadCategory category, String filename) {
        return everythingExists || stored.contains(key(category, filename));
    }

    @Override
    public void delete(UploadCategory category, String filename) {
        stored.remove(key(category, filename));
    }

    @Override
    public Optional<URI> downloadUrl(UploadCategory category, String filename) {
        return Optional.empty();
    }

    @Override
    public Resource loadAsResource(UploadCategory category, String filename) {
        return null;
    }

    private String key(UploadCategory category, String filename) {
        return category.segment() + "/" + filename;
    }
}
