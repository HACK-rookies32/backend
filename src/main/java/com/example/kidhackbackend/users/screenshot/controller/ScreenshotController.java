package com.example.kidhackbackend.users.screenshot.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.screenshot.dto.ScreenshotPageResponse;
import com.example.kidhackbackend.users.screenshot.dto.ScreenshotUploadResponse;
import com.example.kidhackbackend.users.screenshot.entity.CaptureTrigger;
import com.example.kidhackbackend.users.screenshot.service.ScreenshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 자녀 화면 캡처 API — 명세서 4.6.
 *
 * <p>업로드는 자녀 앱(CHILD)이, 목록 조회와 추가 촬영 요청은 부모(PARENT)가 쓴다.
 * 업로드는 명세대로 multipart 이며 presigned URL 을 쓰지 않는다.
 */
@Tag(name = "10. 자녀 화면 캡처", description = "자녀 앱이 캡처 업로드(CHILD) · 부모가 목록 조회/추가 촬영 요청(PARENT)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/children/{childId}/screenshots")
@RequiredArgsConstructor
public class ScreenshotController {

    private final ScreenshotService screenshotService;

    /**
     * 캡처 이미지 업로드(자녀 앱 → 서버).
     *
     * <p>1시간 주기 자동 촬영 또는 부모 요청 추가 촬영. 이미지는 S3 저장, 메타데이터는
     * DB 기록.
     */
    // [VULN-SS-1] 경로 childId 소유권 미검증 → 임의 childId 로 남의 자녀 캡처를 위조 주입 가능(IDOR).
    //   기존 자녀 도메인(VULN-CHILD-*, VULN-MON-1)과 동일 계열.
    @Operation(summary = "캡처 이미지 업로드",
            description = """
                    `multipart/form-data`. 자녀(CHILD) 전용, 성공 시 201.

                    | 폼 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `file` | ✅ | 캡처 이미지 파일 |
                    | `capturedAt` | ✅ | 촬영 시각(ISO-8601) |
                    | `trigger` | ✅ | `SCHEDULED`(1시간 주기 자동) / `MANUAL`(부모 요청) |
                    | `childId` (path) | ✅ | 자녀 회원 ID |

                    ⚠️ [VULN-SS-1] 경로 childId 소유권 미검증(IDOR). [VULN-SS-2] 파일명 미새니타이즈 → 로컬 저장 모드 directory traversal. [VULN-10] 확장자 화이트리스트 없음.""")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ScreenshotUploadResponse>> upload(
            @PathVariable Long childId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("capturedAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime capturedAt,
            @RequestParam("trigger") CaptureTrigger trigger) {

        ScreenshotUploadResponse data = screenshotService.upload(childId, file, capturedAt, trigger);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data));
    }

    /**
     * 캡처 목록 조회(부모). from/to 가 없으면 전체 기간이다.
     */
    // [VULN-SS-1] 경로 childId 소유권 미검증 → 임의 childId 로 남의 자녀 캡처 열람 가능(IDOR).
    @Operation(summary = "캡처 목록 조회",
            description = "부모(PARENT) 전용. from/to(둘 다 있어야 기간 필터 적용)·page·size 페이징, 최신 촬영순. "
                    + "⚠️ [VULN-SS-1] 경로 childId 소유권 미검증 → 남의 자녀 캡처 열람 가능(IDOR).")
    @GetMapping
    public ResponseEntity<ApiResponse<ScreenshotPageResponse>> list(
            @PathVariable Long childId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        ScreenshotPageResponse data = screenshotService.list(childId, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 추가 촬영 요청(부모 → 자녀 앱 푸시).
     *
     * <p>푸시만 보내고 끝난다. 실제 이미지는 자녀 앱이 곧이어 MANUAL 캡처를 업로드해
     * 완성된다. 그래서 202(Accepted)로 응답한다.
     */
    // [VULN-SS-1] 경로 childId 소유권 미검증 → 임의 childId 로 남의 자녀 기기에 촬영 트리거 가능(IDOR).
    @Operation(summary = "추가 촬영 요청",
            description = "부모(PARENT) 전용. 자녀 앱에 즉시 촬영 푸시만 보내고 202. 실제 이미지는 자녀 앱이 MANUAL 캡처를 업로드해 완성. "
                    + "⚠️ [VULN-SS-1] 경로 childId 소유권 미검증 → 남의 자녀 기기에 촬영 트리거 가능(IDOR), rate limit 없음.")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Void>> requestCapture(@PathVariable Long childId) {
        screenshotService.requestCapture(childId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(null));
    }
}
