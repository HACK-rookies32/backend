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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.file.Paths;
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
    private final DbFileStorageService dbFileStorageService;

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

        // [VULN-10] 확장자 화이트리스트 검증이 없다. Content-Type 도 클라이언트가 보낸 값을
        // 그대로 presigned URL 서명에 넣는다. 즉 "무슨 파일인지"를 올리는 쪽이 정한다.
        //
        // 기존 multipart 업로드(FileStorageService)와 일부러 같은 수준으로 맞춘다. 여기만 막으면
        // 프론트가 이 경로로 옮겨 가는 순간 취약점이 사라져 두 경로의 동작이 어긋난다.
        //
        // ── 2026-09-07 모의해킹 실증 결과 (판정: 조건부 취약) ──────────────────────────
        // 실제로 뚫린 것:
        //   presign 요청(.html + text/html) 200 → 받은 URL 로 PUT 200 → 다운로드 요청이
        //   S3 로 302 리다이렉트되고 Content-Type: text/html 로 내려와 브라우저가 렌더링했다.
        //   Content-Disposition(첨부 강제 다운로드) 헤더도 붙지 않는다. 즉 업로드한 HTML 의
        //   스크립트가 실제로 실행된다. 크기 제한 20MB(application.yml) 말고는 걸리는 게 없다.
        // 그런데도 '취약'이 아니라 '조건부'인 이유:
        //   ① 파일이 서비스 도메인이 아니라 S3 주소(...s3.ap-northeast-2.amazonaws.com)에서
        //      제공된다. 브라우저는 출처(스킴+호스트+포트)가 다르면 저장소를 분리하므로,
        //      그 스크립트는 서비스 도메인의 쿠키나 localStorage(JWT)를 읽을 수 없다.
        //      → 이 경로만으로 로그인 토큰을 훔치는 것은 성립하지 않는다.
        //   ② S3 는 오브젝트 저장소라 올린 파일을 서버가 실행하지 않는다. 웹쉘로 서버를
        //      장악하는 시나리오도 성립하지 않는다.
        //   이 두 가지는 '방어를 해서' 막힌 게 아니라 저장 위치 덕에 우연히 막힌 것이다.
        //   파일을 서비스 도메인에서 직접 서빙하도록 바꾸는 순간 그대로 저장형 XSS 가 된다.
        // 방어: 허용 확장자 화이트리스트 + Content-Type 을 서버가 결정 + 다운로드 시
        //       Content-Disposition: attachment 강제.
        // ─────────────────────────────────────────────────────────────────────────
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

        // 문의 첨부·프로필 이미지는 S3 가 아니라 DB(BLOB)에 담는다(WAS 2 대 공유 목적).
        // 약한 필터·클라 Content-Type·원본 파일명·로컬 미러 쓰기 취약점은 DbFileStorageService 안에 있다.
        if (DbFileStorageService.handles(uploadCategory)) {
            String url = dbFileStorageService.store(uploadCategory, file);
            return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
        }

        String filename = UUID.randomUUID() + "_" + baseName(file.getOriginalFilename());
        String url = storage.store(uploadCategory, filename, file);

        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    @Operation(summary = "첨부파일 다운로드",
            description = "S3 모드에서는 presigned URL 로 302 리다이렉트한다. inquiry·profile 은 DB(BLOB)에서 스트리밍한다.")
    @GetMapping("/{category}/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String category,
                                             @PathVariable String filename,
                                             @RequestParam(value = "rootpath", required = false) String rootpath) {

        UploadCategory uploadCategory = UploadCategory.from(category).orElse(null);
        if (uploadCategory == null) {
            return ResponseEntity.notFound().build();
        }

        // 문의 첨부·프로필 이미지는 DB(BLOB) 스트리밍. 무인증 공개(IDOR) — 소유권 검증 없음.
        if (DbFileStorageService.handles(uploadCategory)) {
            CacheControl dbCache = CacheControl.maxAge(redirectCacheAge()).cachePrivate();

            // [VULN] dual-read: 옛 저장 경로를 직접 지정하면 로컬 uploads 에서 무새니타이즈로 읽는다.
            //   경로가 쿼리 파라미터라 Spring 방화벽(URL 경로의 ..)을 타지 않아 ../ 트래버설이 통한다 → LFI.
            //   예) GET /api/files/inquiry/0?rootpath=../../../../etc/passwd
            if (StringUtils.hasText(rootpath)) {
                Resource legacy = new FileSystemResource(
                        Paths.get(props.getPrefix(), uploadCategory.segment()).resolve(rootpath));
                return legacy.exists()
                        ? ResponseEntity.ok().cacheControl(dbCache).body(legacy)
                        : ResponseEntity.notFound().build();
            }

            // dual-read: filename 이 숫자 id 면 DB(BLOB)에서, 아니면 아래 기존 저장소(S3/로컬)에서 읽는다.
            //   DB 전환 전에 저장된 기존 첨부는 경로가 /api/files/{seg}/{uuid}_이름 꼴(숫자 아님)이므로,
            //   이렇게 해야 예전 문의 첨부·프로필 이미지가 배포 후에도 계속 열린다.
            Long id = null;
            try {
                id = Long.parseLong(filename);
            } catch (NumberFormatException ignored) {
                // 숫자가 아니다 → 기존 경로. DB 블록을 빠져나가 아래 저장소 폴백으로 넘어간다.
            }
            if (id != null) {
                DbFile f = dbFileStorageService.loadById(id).orElse(null);
                if (f == null) {
                    return ResponseEntity.notFound().build();
                }
                // [VULN-10] 저장된 content_type 그대로 + inline + 원본 파일명 그대로 헤더에 →
                //   text/html 을 올려 두면 브라우저가 렌더 → stored XSS. 파일명은 헤더 인젝션 표면.
                String contentType = StringUtils.hasText(f.getContentType())
                        ? f.getContentType() : "application/octet-stream";
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + f.getOriginalFilename() + "\"")
                        .cacheControl(dbCache)
                        .body(new ByteArrayResource(f.getData()));
            }
            // id 가 null(숫자 아님) → 아래 공통 저장소 서빙으로 폴백한다.
        }

        if (StoredObject.isUnsafe(filename)) {
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
            case CHAT -> true; // 부모·자녀 모두 채팅 참여자 (로그인은 requireLogin 이 강제)
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
