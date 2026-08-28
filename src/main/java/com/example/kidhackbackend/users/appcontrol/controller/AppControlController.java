package com.example.kidhackbackend.users.appcontrol.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.appcontrol.dto.*;
import com.example.kidhackbackend.users.appcontrol.service.AppControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "07. 앱 통제", description = "자녀 설치 앱 동기화/조회 · 앱별 사용시간 제한 규칙 CRUD")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/appcontrol")
@RequiredArgsConstructor
public class AppControlController {

    private final AppControlService appControlService;

    @Operation(summary = "설치 앱 목록 동기화 (자녀 앱)",
            description = "자녀 앱이 설치된 앱 목록을 서버에 올린다. 반영된 건수를 반환한다.")
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Integer>> sync(@Valid @RequestBody SyncRequest request) {
        return ResponseEntity.ok(ApiResponse.success(appControlService.sync(request)));
    }

    @Operation(summary = "자녀 설치 앱 목록 조회 (부모)")
    @GetMapping("/apps/{childId}")
    public ResponseEntity<ApiResponse<List<AppResponse>>> getInstalledApps(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        List<AppResponse> data = appControlService.getInstalledApps(childId).stream()
                .map(AppResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "통제 규칙 추가 (부모)",
            description = "패키지명 + 일일 사용시간(분) 제한 규칙을 추가한다.")
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<RuleResponse>> addRule(@Valid @RequestBody RuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                RuleResponse.from(appControlService.addRule(
                        request.childId(), request.packageName(),
                        request.appName(), request.dailyLimitMinutes()))));
    }

    @Operation(summary = "통제 규칙 목록 조회 (부모/자녀)")
    @GetMapping("/rules/{childId}")
    public ResponseEntity<ApiResponse<List<RuleResponse>>> getRules(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        List<RuleResponse> data = appControlService.getRules(childId).stream()
                .map(RuleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "통제 규칙 수정 (부모)",
            description = "일일 제한 시간 / 활성화 여부를 수정한다.")
    @PatchMapping("/rules/{ruleId}")
    public ResponseEntity<ApiResponse<RuleResponse>> updateRule(
            @Parameter(description = "규칙 ID") @PathVariable Long ruleId,
            @RequestBody RuleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                RuleResponse.from(appControlService.updateRule(
                        ruleId, request.dailyLimitMinutes(), request.enabled()))));
    }

    @Operation(summary = "통제 규칙 삭제 (부모)")
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @Parameter(description = "규칙 ID") @PathVariable Long ruleId) {
        appControlService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
