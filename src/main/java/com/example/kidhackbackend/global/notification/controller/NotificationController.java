package com.example.kidhackbackend.global.notification.controller;

import com.example.kidhackbackend.global.notification.dto.NotificationResponse;
import com.example.kidhackbackend.global.notification.service.NotificationService;
import com.example.kidhackbackend.global.response.ApiResponse;
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
@Tag(name = "Notification", description = "알림 조회 API (공통) — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 수신자의 알림 목록(최신순). */
    @GetMapping("/{recipientId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(@PathVariable Long recipientId) {
        List<NotificationResponse> data = notificationService.findByRecipient(recipientId).stream()
                .map(NotificationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** 알림 읽음 처리. */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> read(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 수신자의 안읽음 알림 개수(폴링용). */
    @GetMapping("/{recipientId}/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@PathVariable Long recipientId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.countUnread(recipientId)));
    }
}
