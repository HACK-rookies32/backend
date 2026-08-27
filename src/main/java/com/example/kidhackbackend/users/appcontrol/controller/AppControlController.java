package com.example.kidhackbackend.users.appcontrol.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.appcontrol.dto.*;
import com.example.kidhackbackend.users.appcontrol.service.AppControlService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AppControl", description = "앱 통제 API — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/appcontrol")
@RequiredArgsConstructor
public class AppControlController {

    private final AppControlService appControlService;

    /** 자녀 설치 앱 목록 동기화 (자녀 앱 → 서버). */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Integer>> sync(@Valid @RequestBody SyncRequest request) {
        return ResponseEntity.ok(ApiResponse.success(appControlService.sync(request)));
    }

    /** 자녀 설치 앱 목록 조회 (부모). */
    @GetMapping("/apps/{childId}")
    public ResponseEntity<ApiResponse<List<AppResponse>>> getInstalledApps(@PathVariable Long childId) {
        List<AppResponse> data = appControlService.getInstalledApps(childId).stream()
                .map(AppResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** 통제 규칙 추가 (부모). */
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<RuleResponse>> addRule(@Valid @RequestBody RuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                RuleResponse.from(appControlService.addRule(
                        request.childId(), request.packageName(),
                        request.appName(), request.dailyLimitMinutes()))));
    }

    /** 통제 규칙 목록 조회 (부모/자녀). */
    @GetMapping("/rules/{childId}")
    public ResponseEntity<ApiResponse<List<RuleResponse>>> getRules(@PathVariable Long childId) {
        List<RuleResponse> data = appControlService.getRules(childId).stream()
                .map(RuleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** 통제 규칙 수정 (부모). */
    @PatchMapping("/rules/{ruleId}")
    public ResponseEntity<ApiResponse<RuleResponse>> updateRule(
            @PathVariable Long ruleId,
            @RequestBody RuleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                RuleResponse.from(appControlService.updateRule(
                        ruleId, request.dailyLimitMinutes(), request.enabled()))));
    }

    /** 통제 규칙 삭제 (부모). */
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long ruleId) {
        appControlService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}