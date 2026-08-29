package com.example.kidhackbackend.users.inquiry.controller;

import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
import com.example.kidhackbackend.global.exception.InvalidCredentialException;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryAttachmentUploadResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryDetailResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryPageResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryRequest;
import com.example.kidhackbackend.users.inquiry.dto.InquiryResponse;
import com.example.kidhackbackend.users.inquiry.service.InquiryFileStorageService;
import com.example.kidhackbackend.users.inquiry.service.InquiryService;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "21. 1:1 문의 (회원)", description = "회원이 본인 문의 등록/조회/수정/삭제 · 첨부 업로드 — PARENT 전용")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final InquiryFileStorageService fileStorageService;

    private void requireParent(Member member) {
        if (member == null) {
            throw new InvalidCredentialException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.PARENT) {
            throw new AdminAccessDeniedException("부모 회원만 이용할 수 있는 기능입니다.");
        }
    }

    @Operation(summary = "문의 등록",
            description = """
                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `title` | ✅ | 제목 |
                    | `content` | ✅ | 본문 |
                    | `attachments` | — | 첨부 공개 경로 목록. 먼저 `POST /api/inquiries/upload` 로 업로드해 받은 `url` 을 넣는다 |
                    | `linkUrl` | — | 관련 링크 URL |
                    """)
    @PostMapping
    public ResponseEntity<ApiResponse<InquiryResponse>> createInquiry(
            @AuthenticationPrincipal Member member,
            @RequestBody @Valid InquiryRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(inquiryService.createInquiry(member, request)));
    }

    @Operation(summary = "문의 목록 조회 (본인 문의만, 최신순 페이징)",
            description = """
                    로그인한 회원이 작성한 문의만 최신순으로 반환한다.

                    | 파라미터 | 필수 | 설명 |
                    |---|:---:|---|
                    | `page` / `size` | — | 페이징(기본 0 / 20) |
                    """)
    @GetMapping
    public ResponseEntity<ApiResponse<InquiryPageResponse>> getMyInquiries(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getMyInquiries(member, page, size)));
    }

    @Operation(summary = "문의 상세 조회",
            description = "본문·첨부·링크 + 관리자 답변(없으면 null)을 함께 반환한다. 없는 id → 404. "
                    + "⚠️ [VULN-9] 작성자 미검증(IDOR) — id 만 알면 남의 문의도 열람 가능.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InquiryDetailResponse>> getInquiryDetail(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "문의 ID") @PathVariable Long id) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getInquiryDetail(id)));
    }

    @Operation(summary = "문의 수정",
            description = "제목·본문·첨부·링크를 통째로 교체한다. 없는 id → 404, **답변이 완료된 문의는 수정 불가(400)**. "
                    + "⚠️ [VULN-9] 작성자 미검증(IDOR) — id 만 알면 남의 문의도 수정 가능.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InquiryResponse>> updateInquiry(
            @Parameter(description = "문의 ID") @PathVariable Long id,
            @AuthenticationPrincipal Member member,
            @RequestBody @Valid InquiryRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(inquiryService.updateInquiry(id, member, request)));
    }

    @Operation(summary = "문의 삭제",
            description = "문의와 첨부 실물, 답변을 함께 삭제한다. 없는 id → 404, **답변이 완료된 문의는 삭제 불가(400)**. "
                    + "⚠️ [VULN-9] 작성자 미검증(IDOR) — id 만 알면 남의 문의도 삭제 가능(첨부 파일까지).")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInquiry(
            @Parameter(description = "문의 ID") @PathVariable Long id,
            @AuthenticationPrincipal Member member) {
        requireParent(member);
        inquiryService.deleteInquiry(id, member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "첨부파일 업로드",
            description = """
                    `multipart/form-data` 로 파일 하나를 업로드하고 공개 경로(`url`)를 받는다.
                    이 `url` 을 문의 등록/수정의 `attachments` 배열에 넣는다.

                    저장 흐름: WAS 가 파일을 받아 `ObjectStorage`(운영 S3 / 로컬 디스크)에 저장 →
                    `/api/files/inquiry/{filename}` 경로 반환 → 조회 시 presigned S3 GET 으로 302 리다이렉트.

                    ⚠️ [VULN-10] 확장자 화이트리스트 없음 + Content-Type 을 클라이언트 값 그대로 사용 + 파일명 미새니타이즈.
                    """)
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<InquiryAttachmentUploadResponse>> uploadFile(
            @AuthenticationPrincipal Member member,
            @RequestParam("file") MultipartFile file) {
        requireParent(member);
        String url = fileStorageService.store(file);
        return ResponseEntity.ok(ApiResponse.success(new InquiryAttachmentUploadResponse(url)));
    }
}
