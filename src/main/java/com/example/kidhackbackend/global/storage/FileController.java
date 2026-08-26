package com.example.kidhackbackend.global.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 첨부파일 다운로드.
 *
 * <p>업로드 API 가 돌려준 경로({@code /api/files/{category}/{filename}})가 그대로
 * 이 컨트롤러로 들어온다. 그 문자열은 PostDetailResponse.attachments 와
 * BoardDetailResponse.attachmentUrl 로 조회 응답에도 실려 나가므로, 프론트는
 * 받은 값을 {@code <a href>} 나 {@code <img src>} 에 그대로 쓰면 된다.
 *
 * <p><b>왜 S3 URL 을 직접 내려주지 않는가.</b> 버킷은 퍼블릭 접근이 차단되어 있어
 * 서명 없이는 받을 수 없고, presigned URL 은 만료되므로 DB 에 저장할 수 없다.
 * 요청 시점에 발급해 줄 자리가 필요하다.
 *
 * <p>덤으로, URL 을 그대로 넘기지 않으면 서버가 접근을 통제할 지점이 남는다.
 * 지금은 SecurityConfig 가 permitAll 이라 실제 검사가 없지만, 넣을 자리는
 * 아래에 표시해 두었다.
 */
@Tag(name = "첨부파일")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final ObjectStorage storage;

    @Operation(summary = "첨부파일 다운로드",
            description = "S3 모드에서는 presigned URL 로 302 리다이렉트한다.")
    @GetMapping("/{category}/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String category,
                                             @PathVariable String filename) {

        UploadCategory uploadCategory = UploadCategory.from(category).orElse(null);
        if (uploadCategory == null || isTraversal(filename)) {
            return ResponseEntity.notFound().build();
        }

        // 소유권 검사를 넣는다면 이 자리다.
        // 첨부를 참조하는 게시글/문의를 역조회해 요청자와 대조하면 된다.

        return storage.downloadUrl(uploadCategory, filename)
                .map(uri -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(uri)
                        .<Resource>build())
                .orElseGet(() -> {
                    Resource resource = storage.loadAsResource(uploadCategory, filename);
                    return resource.exists()
                            ? ResponseEntity.ok(resource)
                            : ResponseEntity.notFound().build();
                });
    }

    /**
     * 경로 탈출 차단.
     *
     * <p>업로드 시 파일명에 원본 이름이 그대로 들어가므로([VULN-10]) 구분자나
     * 상위 참조가 섞여 들어올 수 있다. 여기서 막지 않으면 로컬 모드에서
     * 저장 디렉터리를 벗어난 파일을 읽을 수 있다.
     *
     * <p>이건 의도적으로 심은 취약점이 아니라 새로 만드는 읽기 경로이므로 막는다.
     */
    private boolean isTraversal(String filename) {
        return filename.contains("..")
                || filename.contains("/")
                || filename.contains("\\");
    }
}
