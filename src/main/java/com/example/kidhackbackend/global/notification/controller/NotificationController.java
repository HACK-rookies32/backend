package com.example.kidhackbackend.global.notification.controller;

import com.example.kidhackbackend.global.notification.dto.NotificationResponse;
import com.example.kidhackbackend.global.notification.service.NotificationService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 알림 조회 API (공통 도메인).
 */
@Tag(name = "24. 알림", description = "수신자별 알림 목록/안읽음 개수 조회 · 읽음 처리")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회",
            description = "수신자(recipientId)의 알림을 최신순으로 조회한다. ⚠️ recipientId 소유권 미검증(IDOR).")
    @GetMapping("/{recipientId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(
            @Parameter(description = "수신자 회원 ID") @PathVariable Long recipientId) {
        List<NotificationResponse> data = notificationService.findByRecipient(recipientId).stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> read(
            @Parameter(description = "알림 ID") @PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "안읽음 알림 개수 (폴링용)",
            description = "수신자의 읽지 않은 알림 개수를 반환한다.")
    @GetMapping("/{recipientId}/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            @Parameter(description = "수신자 회원 ID") @PathVariable Long recipientId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.countUnread(recipientId)));
    }
}
