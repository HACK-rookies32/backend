package com.example.kidhackbackend.admin.notice.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.notice.dto.AdminNoticeDetailResponse;
import com.example.kidhackbackend.admin.notice.dto.AdminNoticePageResponse;
import com.example.kidhackbackend.global.notice.dto.NoticeRequest;
import com.example.kidhackbackend.global.notice.service.NoticeService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 공지 CRUD.
 *
 * <p>여기서 공지를 써도 <b>알림은 나가지 않는다.</b> 전체 알림은
 * {@code POST /api/admin/notifications/broadcast} 가 담당한다. 관리자 웹도 「공지 작성」과
 * 「전체 알림 발송」을 별도 버튼으로 두고 있다.
 */
@Tag(name = "33. 관리자 - 공지", description = "공지 목록/상세 조회 · 생성/수정/삭제 (관리자). 공지 작성만으로는 알림이 나가지 않음")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;
    private final AdminAuditLogService adminAuditLogService;

    @Operation(summary = "공지 목록 조회 (관리자)",
            description = "최신순 페이징. `keyword` 로 제목 부분 검색(대소문자 무시).")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminNoticePageResponse>> getNotices(
            @Parameter(description = "제목 검색어") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.getNoticesForAdmin(keyword, PageRequest.of(page, size))));
    }

    @Operation(summary = "공지 상세 조회 (관리자)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminNoticeDetailResponse>> getNotice(
            @Parameter(description = "공지 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNoticeForAdmin(id)));
    }

    @Operation(summary = "공지 생성", description = "생성 시 감사 로그(NOTICE_CREATE)를 남긴다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminNoticeDetailResponse>> createNotice(
            @AuthenticationPrincipal Member member, @RequestBody @Valid NoticeRequest request) {
        AdminNoticeDetailResponse response = noticeService.createNotice(member, request);
        adminAuditLogService.record(member, AdminActivityType.NOTICE_CREATE, response.id());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공지 수정", description = "수정 시 감사 로그(NOTICE_UPDATE)를 남긴다.")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminNoticeDetailResponse>> updateNotice(
            @Parameter(description = "공지 ID") @PathVariable Long id, @RequestBody @Valid NoticeRequest request) {
        AdminNoticeDetailResponse response = noticeService.updateNotice(id, request);
        adminAuditLogService.record(AdminActivityType.NOTICE_UPDATE, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공지 삭제", description = "삭제 시 감사 로그(NOTICE_DELETE)를 남긴다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
            @Parameter(description = "공지 ID") @PathVariable Long id) {
        noticeService.deleteNotice(id);
        adminAuditLogService.record(AdminActivityType.NOTICE_DELETE, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
