package com.example.kidhackbackend.global.storage;

import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
import com.example.kidhackbackend.global.exception.InvalidCredentialException;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.global.storage.dto.PresignRequest;
import com.example.kidhackbackend.global.storage.dto.PresignResponse;
import com.example.kidhackbackend.global.storage.dto.UploadCompleteRequest;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * 첨부파일 공용 엔드포인트 — presigned 업로드 발급 / 완료 확인 / 다운로드.
 *
 * <p>업로드 API 가 돌려준 경로({@code /api/files/{category}/{filename}})가 그대로
 * 이 컨트롤러로 들어온다. 그 문자열은 PostDetailResponse.attachments 와
 * InquiryDetailResponse.attachments 로 조회 응답에도 실려 나가므로, 프론트는
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
@Tag(name = "26. 첨부파일", description = "presigned 업로드 URL 발급 · 완료 확인 · 다운로드")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final ObjectStorage storage;
    private final StorageProperties props;

    /**
     * presigned 업로드 URL 발급.
     *
     * <p>이 뒤로 파일 본문은 브라우저에서 S3 로 바로 간다. WAS 2 대의 대역폭과
     * 요청 스레드를 쓰지 않고, ALB → Nginx → NLB 3 단 프록시의 바디 크기 제한과
     * 타임아웃도 우회한다.
     *
     * <p>발급 자체가 버킷에 쓸 권한을 넘겨주는 행위이므로 로그인 회원만 받는다.
     * SecurityConfig 가 permitAll 이라 필터가 막아 주지 않으므로 여기서 확인한다
     * (다른 컨트롤러들과 같은 방식이다).
     *
     * <p>저장 파일명은 반드시 서버가 만든다. 클라이언트가 준 이름을 그대로 키에
     * 넣으면 {@code ../} 로 prefix 밖의 객체를 덮어쓸 수 있다.
     */
    @Operation(summary = "첨부파일 업로드 URL 발급",
            description = "S3 모드에서만 동작한다. 로컬 디스크 모드에서는 409 를 주므로 기존 multipart 업로드를 쓸 것. "
                    + "분류별 권한: community·inquiry=PARENT, notice=관리자, screenshot=CHILD, profile=로그인 회원.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{category}/presign")
    public ResponseEntity<ApiResponse<PresignResponse>> presign(
            @AuthenticationPrincipal Member member,
            @PathVariable String category,
            @RequestBody @Valid PresignRequest request) {

        requireLogin(member);
        UploadCategory uploadCategory = requireCategory(category);
        requireUploadPermission(member, uploadCategory);

        // [VULN-10] 확장자 화이트리스트 검증이 없다.
        //
        // 기존 multipart 업로드(FileStorageService)와 일부러 같은 수준으로 맞춘다.
        // 여기만 막으면 프론트가 이 경로로 옮겨 가는 순간 취약점이 사라져 두 경로의
        // 동작이 어긋난다. Content-Type 도 마찬가지로 클라이언트 값을 그대로 서명에
        // 넣는다.
        String contentType = StringUtils.hasText(request.getContentType())
                ? request.getContentType()
                : "application/octet-stream";

        String filename = UUID.randomUUID() + "_" + baseName(request.getFilename());

        URI uploadUrl = storage.uploadUrl(uploadCategory, filename, contentType)
                .orElse(null);

        if (uploadUrl == null) {
            // 로컬 디스크 모드. 서명할 대상이 없다.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("PRESIGN_UNSUPPORTED",
                            "현재 저장소에서는 직접 업로드를 쓸 수 없습니다. multipart 업로드 API 를 사용하세요."));
        }

        StoredObject stored = new StoredObject(uploadCategory, filename);
        return ResponseEntity.ok(ApiResponse.success(new PresignResponse(
                uploadUrl.toString(),
                stored.publicPath(),   // 응답 필드명은 url — multipart/complete API 와 동일
                filename,
                contentType,
                props.getPresignTtl().toSeconds())));
    }

    /**
     * 업로드 완료 확인.
     *
     * <p>presigned PUT 은 WAS 를 지나가지 않으므로 서버는 성공 여부를 모른다.
     * 클라이언트 말만 믿고 경로를 돌려주면 DB 에 실재하지 않는 파일이 박히므로
     * S3 에 실제로 있는지 확인한 뒤에만 공개 경로를 확정한다.
     *
     * <p>응답 모양을 기존 multipart 업로드 API 와 똑같이 {@code {"url": ...}} 로
     * 맞춰 두었다. 프론트가 두 경로를 같은 코드로 이어 붙일 수 있다.
     */
    @Operation(summary = "첨부파일 업로드 완료 확인",
            description = "S3 에 실제로 올라갔는지 확인하고 DB 에 넣을 공개 경로를 확정한다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{category}/complete")
    public ResponseEntity<ApiResponse<Map<String, String>>> complete(
            @AuthenticationPrincipal Member member,
            @PathVariable String category,
            @RequestBody @Valid UploadCompleteRequest request) {

        requireLogin(member);
        UploadCategory uploadCategory = requireCategory(category);
        requireUploadPermission(member, uploadCategory);

        String filename = request.getFilename();
        if (StoredObject.isUnsafe(filename)) {
            throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
        }

        if (!storage.exists(uploadCategory, filename)) {
            throw new ResourceNotFoundException("업로드된 파일을 찾을 수 없습니다.");
        }

        String url = new StoredObject(uploadCategory, filename).publicPath();
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    /**
     * multipart 업로드 — 파일 본문이 WAS 를 거쳐 저장소로 간다.
     *
     * <p>presign 은 S3 모드에서만 동작하고 로컬 디스크 모드에서는 409 를 주면서
     * "multipart 를 쓰라"고 안내한다. 그런데 그 multipart 가 커뮤니티·문의에만 있어
     * <b>공지 첨부는 로컬에서 올릴 방법이 아예 없었다.</b> 분류별로 하나씩 만들지 않고
     * 여기 하나로 둔다 — 권한 규칙과 파일명 생성이 이미 분류별로 정리돼 있다.
     *
     * <p>응답은 presign 완료·기존 업로드들과 같은 {@code {"url": ...}} 이다. 프론트는
     * 받은 값을 그대로 공지·게시글·문의의 {@code attachments} 에 넣으면 된다.
     *
     * <p>저장 파일명은 반드시 서버가 만든다. 클라이언트가 준 이름을 그대로 키에 넣으면
     * {@code ../} 로 prefix 밖의 객체를 덮어쓸 수 있다.
     */
    @Operation(summary = "첨부파일 업로드 (multipart)",
            description = """
                    파일을 서버로 직접 올리고 공개 경로를 받는다. **S3·로컬 디스크 모두에서 동작한다** —
                    presign 이 409 를 주는 로컬 환경에서는 이 API 를 쓴다.

                    | 요청 | 필수 | 설명 |
                    |---|:---:|---|
                    | `category` (경로) | ✅ | `community` / `inquiry` / `notice` / `profile` / `screenshot` |
                    | `file` (multipart) | ✅ | 올릴 파일 하나 |

                    분류별 권한: community·inquiry=PARENT, notice=관리자, screenshot=CHILD, profile=로그인 회원.

                    응답 `url` 을 공지·게시글·문의 작성의 `attachments` 배열에 넣는다.

                    ⚠️ [VULN-10] 확장자 화이트리스트가 없고 Content-Type 을 클라이언트 값 그대로 쓴다
                    (기존 multipart 업로드와 같은 수준).
                    """)
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/{category}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @AuthenticationPrincipal Member member,
            @PathVariable String category,
            @RequestPart("file") MultipartFile file) {

        requireLogin(member);
        UploadCategory uploadCategory = requireCategory(category);
        requireUploadPermission(member, uploadCategory);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        String filename = UUID.randomUUID() + "_" + baseName(file.getOriginalFilename());
        String url = storage.store(uploadCategory, filename, file);

        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    @Operation(summary = "첨부파일 다운로드",
            description = "S3 모드에서는 presigned URL 로 302 리다이렉트한다.")
    @GetMapping("/{category}/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String category,
                                             @PathVariable String filename) {

        UploadCategory uploadCategory = UploadCategory.from(category).orElse(null);
        if (uploadCategory == null || StoredObject.isUnsafe(filename)) {
            return ResponseEntity.notFound().build();
        }

        // 소유권 검사를 넣는다면 이 자리다.
        // 첨부를 참조하는 게시글/문의를 역조회해 요청자와 대조하면 된다.

        // 브라우저가 결과를 잠시 재사용하도록 Cache-Control 을 싣는다. 이게 없으면
        // Spring Security 기본 헤더 라이터가 no-store 를 붙여 <img src> 가 매번 왕복한다.
        // 302 의 경우 리다이렉트 대상 presigned URL 이 캐시 도중 만료되지 않게 서명 기간보다
        // 짧게 잡는다.
        CacheControl cacheControl = CacheControl.maxAge(redirectCacheAge()).cachePrivate();

        return storage.downloadUrl(uploadCategory, filename)
                .map(uri -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(uri)
                        .cacheControl(cacheControl)
                        .<Resource>build())
                .orElseGet(() -> {
                    Resource resource = storage.loadAsResource(uploadCategory, filename);
                    return resource.exists()
                            ? ResponseEntity.ok().cacheControl(cacheControl).body(resource)
                            : ResponseEntity.notFound().build();
                });
    }

    /** presigned GET 서명 기간에서 5 분 여유를 뺀 값. 최소 0. */
    private Duration redirectCacheAge() {
        Duration ttl = props.getDownloadTtl().minusMinutes(5);
        return ttl.isNegative() ? Duration.ZERO : ttl;
    }

    private void requireLogin(Member member) {
        if (member == null) {
            throw new InvalidCredentialException("로그인이 필요합니다.");
        }
    }

    private UploadCategory requireCategory(String category) {
        return UploadCategory.from(category)
                .orElseThrow(() -> new ResourceNotFoundException("알 수 없는 첨부 분류입니다: " + category));
    }

    /**
     * 분류별 업로드 권한. presign 발급은 곧 버킷에 쓸 권한을 넘기는 것이므로, 다른
     * 도메인 네임스페이스(관리자 공지·자녀 캡처)에 아무나 올리지 못하게 막는다.
     */
    private void requireUploadPermission(Member member, UploadCategory category) {
        boolean allowed = switch (category) {
            case COMMUNITY, INQUIRY -> member.getRole() == Role.PARENT;
            case SCREENSHOT -> member.getRole() == Role.CHILD;
            case NOTICE -> member.getRole().name().endsWith("ADMIN");
            case PROFILE -> true; // 로그인한 회원이면 자기 프로필 이미지
        };
        if (!allowed) {
            throw new AdminAccessDeniedException(
                    "이 분류에 업로드할 권한이 없습니다: " + category.segment());
        }
    }

    /**
     * 원본 파일명에서 디렉터리 부분을 떼어낸다.
     *
     * <p>브라우저는 보통 파일명만 보내지만 클라이언트를 신뢰할 이유가 없다. 여기서
     * 걸러 두면 UUID 접두사와 합쳐진 최종 이름에 구분자가 남지 않는다.
     */
    private String baseName(String original) {
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        return StoredObject.isUnsafe(name) ? "file" : name;
    }
}
