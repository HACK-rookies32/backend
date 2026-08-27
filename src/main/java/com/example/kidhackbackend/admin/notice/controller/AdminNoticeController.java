package com.example.kidhackbackend.admin.notice.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.notice.dto.AdminNoticeDetailResponse;
import com.example.kidhackbackend.admin.notice.dto.AdminNoticeListResponse;
import com.example.kidhackbackend.global.notice.dto.NoticeRequest;
import com.example.kidhackbackend.global.notice.service.NoticeService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;
    private final AdminAuditLogService adminAuditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminNoticeListResponse>>> getNotices(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.getNoticesForAdmin(PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminNoticeDetailResponse>> getNotice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNoticeForAdmin(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminNoticeDetailResponse>> createNotice(
            @AuthenticationPrincipal Member member, @RequestBody NoticeRequest request) {
        AdminNoticeDetailResponse response = noticeService.createNotice(member, request);
        adminAuditLogService.record(member, AdminActivityType.NOTICE_CREATE, response.noticeId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminNoticeDetailResponse>> updateNotice(
            @PathVariable Long id, @RequestBody NoticeRequest request) {
        AdminNoticeDetailResponse response = noticeService.updateNotice(id, request);
        adminAuditLogService.record(AdminActivityType.NOTICE_UPDATE, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        adminAuditLogService.record(AdminActivityType.NOTICE_DELETE, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
