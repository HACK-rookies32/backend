package com.example.kidhackbackend.admin.notification.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.notification.dto.BroadcastRequest;
import com.example.kidhackbackend.admin.notification.service.BroadcastNotificationService;
import com.example.kidhackbackend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final BroadcastNotificationService broadcastNotificationService;
    private final AdminAuditLogService adminAuditLogService;

    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<Void>> broadcast(@RequestBody BroadcastRequest request) {
        broadcastNotificationService.broadcast(request.getMessage());
        // 전체 발송이라 대상 리소스 하나를 특정할 수 없다 — targetId 는 null.
        adminAuditLogService.record(AdminActivityType.NOTIFICATION_BROADCAST, null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}