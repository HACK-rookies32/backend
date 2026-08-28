package com.example.kidhackbackend.admin.notification.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.notification.dto.BroadcastRequest;
import com.example.kidhackbackend.admin.notification.service.BroadcastNotificationService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "34. 관리자 - 알림 발송", description = "전체 회원 대상 알림 브로드캐스트 (공지 저장과는 별개)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final BroadcastNotificationService broadcastNotificationService;
    private final AdminAuditLogService adminAuditLogService;

    @Operation(summary = "전체 알림 발송",
            description = """
                    전체 회원에게 알림을 발송한다. **공지는 저장하지 않는다** — 공지 작성은 `POST /api/admin/notices` 로 따로 하고 그쪽은 알림을 보내지 않는다.
                    감사 로그(NOTIFICATION_BROADCAST)를 남긴다.
                    """)
    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<Void>> broadcast(@Valid @RequestBody BroadcastRequest request) {
        broadcastNotificationService.broadcast(request.getType(), request.getTitle(), request.getBody());
        // 전체 발송이라 대상 리소스 하나를 특정할 수 없다 — targetId 는 null.
        adminAuditLogService.record(AdminActivityType.NOTIFICATION_BROADCAST, null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
