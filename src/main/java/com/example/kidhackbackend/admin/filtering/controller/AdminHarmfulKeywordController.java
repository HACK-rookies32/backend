package com.example.kidhackbackend.admin.filtering.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.filtering.dto.HarmfulKeywordRequest;
import com.example.kidhackbackend.admin.filtering.dto.HarmfulKeywordResponse;
import com.example.kidhackbackend.admin.filtering.service.AdminHarmfulKeywordService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin Harmful Keyword", description = "전역 유해 사이트/키워드 관리 API (SUPER_ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/harmful-keywords")
@RequiredArgsConstructor
public class AdminHarmfulKeywordController {

    private final AdminHarmfulKeywordService adminHarmfulKeywordService;
    private final AdminAuditLogService adminAuditLogService;

    @Operation(summary = "차단 키워드 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HarmfulKeywordResponse>>> getKeywords() {
        return ResponseEntity.ok(ApiResponse.success(adminHarmfulKeywordService.getKeywords()));
    }

    @Operation(summary = "차단 키워드 추가",
            description = "등록한 키워드는 전체 사용자의 차단 판정에 즉시 반영된다.")
    @PostMapping
    public ResponseEntity<ApiResponse<HarmfulKeywordResponse>> addKeyword(
            @Valid @RequestBody HarmfulKeywordRequest request) {
        HarmfulKeywordResponse response = adminHarmfulKeywordService.addKeyword(request);
        adminAuditLogService.record(AdminActivityType.HARMFUL_KEYWORD_ADD, response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "차단 키워드 제외")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteKeyword(@PathVariable Long id) {
        adminHarmfulKeywordService.deleteKeyword(id);
        adminAuditLogService.record(AdminActivityType.HARMFUL_KEYWORD_DELETE, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
