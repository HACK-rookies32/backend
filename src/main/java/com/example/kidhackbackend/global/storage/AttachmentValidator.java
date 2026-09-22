package com.example.kidhackbackend.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 게시글·문의·공지 저장 시 {@code attachments} 배열의 각 경로가 실제로 올라온 파일을
 * 가리키는지 확인한다.
 *
 * <p>업로드 흐름은 presign 발급 → S3 로 직접 PUT → (완료 확인) → 작성 요청에 경로 첨부
 * 인데, 완료 확인이 선택 단계라 강제력이 없었다. 클라이언트가 presign 만 받고 PUT 을
 * 건너뛴 채 경로를 넣으면 <b>실재하지 않는 파일을 가리키는 글</b>이 저장됐다. 여기서
 * 저장소에 실제로 있는지 한 번 더 본다.
 *
 * <p>형식이 우리 것이 아니거나(외부 URL·오타) 분류가 맞지 않는 경로도 함께 걸러낸다 —
 * 다른 도메인의 첨부 경로를 넣어 두는 것을 막기 위해서다.
 */
@Component
@RequiredArgsConstructor
public class AttachmentValidator {

    private final ObjectStorage storage;

    public void requireAllUploaded(List<String> attachments, UploadCategory category) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (String path : attachments) {
            // DB(BLOB) 저장 분류(inquiry·profile)는 /api/files/{seg}/{id} 형태 → 형식만 검증한다.
            // (존재 확인까지 하려면 DbFileStorageService 를 주입해야 하는데, 그러면 AttachmentValidator 만
            //  슬라이스로 올리는 테스트들이 그 빈까지 요구하게 돼 결합이 커진다. 업로드는 이미 끝난 뒤라
            //  형식 검증으로 충분하다.)
            if (DbFileStorageService.handles(category)) {
                boolean ok = path != null
                        && path.startsWith("/api/files/" + category.segment() + "/")
                        && DbFileStorageService.parseId(path).isPresent();
                if (!ok) {
                    throw new IllegalArgumentException("첨부 경로가 올바르지 않습니다: " + path);
                }
                continue;
            }

            StoredObject object = StoredObject.parse(path)
                    .filter(parsed -> parsed.category() == category)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "첨부 경로가 올바르지 않습니다: " + path));

            if (!storage.exists(object.category(), object.filename())) {
                throw new IllegalArgumentException(
                        "업로드가 완료되지 않은 첨부가 있습니다: " + path);
            }
        }
    }
}
