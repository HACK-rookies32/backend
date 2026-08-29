package com.example.kidhackbackend.global.notification.controller;

import com.example.kidhackbackend.global.notification.dto.NotificationPageResponse;
import com.example.kidhackbackend.global.notification.service.NotificationService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 조회 API (공통 도메인).
 */
@Tag(name = "24. 알림", description = "수신자별 알림 목록/안읽음 개수 조회 · 읽음 처리 (부모·자녀 모두)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회 (페이징)",
            description = "수신자(recipientId)의 알림을 최신순으로 페이징 조회한다. ⚠️ recipientId 소유권 미검증(IDOR).")
    @GetMapping("/{recipientId}")
    public ResponseEntity<ApiResponse<NotificationPageResponse>> list(
            @Parameter(description = "수신자 회원 ID") @PathVariable Long recipientId,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotifications(recipientId, PageRequest.of(page, size))));
    }

    @Operation(summary = "알림 읽음 처리", description = "없는 알림 ID → 404.")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> read(
            @Parameter(description = "알림 ID") @PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "알림 모두 읽음 처리",
            description = "수신자의 안 읽은 알림을 한 번에 읽음 처리하고, 처리된 건수를 반환한다. ⚠️ recipientId 소유권 미검증(IDOR).")
    @PatchMapping("/{recipientId}/read-all")
    public ResponseEntity<ApiResponse<Integer>> readAll(
            @Parameter(description = "수신자 회원 ID") @PathVariable Long recipientId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAllAsRead(recipientId)));
    }

    @Operation(summary = "안읽음 알림 개수 (폴링용)",
            description = "수신자의 읽지 않은 알림 개수를 반환한다.")
    @GetMapping("/{recipientId}/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            @Parameter(description = "수신자 회원 ID") @PathVariable Long recipientId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.countUnread(recipientId)));
    }
}
