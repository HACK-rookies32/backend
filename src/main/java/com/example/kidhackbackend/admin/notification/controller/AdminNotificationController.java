package com.example.kidhackbackend.admin.notification.controller;

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

    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<Void>> broadcast(@RequestBody BroadcastRequest request) {
        broadcastNotificationService.broadcast(request.getMessage());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}