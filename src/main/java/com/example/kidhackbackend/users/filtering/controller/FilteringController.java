package com.example.kidhackbackend.users.filtering.controller;

import com.example.kidhackbackend.users.filtering.dto.BlockRuleRequest;
import com.example.kidhackbackend.users.filtering.dto.BlockRuleResponse;
import com.example.kidhackbackend.users.filtering.dto.VisitRequest;
import com.example.kidhackbackend.users.filtering.dto.VisitResponse;
import com.example.kidhackbackend.users.filtering.service.BlockRuleService;
import com.example.kidhackbackend.users.filtering.service.FilteringService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 유해사이트/앱 차단 API — 역할표 No.5(감지)·No.6(부모 차단).
 */
@Tag(name = "Filtering", description = "유해사이트/앱 차단 API — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/filtering")
@RequiredArgsConstructor
public class FilteringController {

    private final BlockRuleService blockRuleService;
    private final FilteringService filteringService;

    /** 차단 규칙 등록(부모) — No.6. */
    // [VULN-FILT-3] 본문의 childId 소유권 미검증 → 남의 자녀에게 임의 차단규칙 등록 가능(IDOR).
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<BlockRuleResponse>> addRule(@Valid @RequestBody BlockRuleRequest request) {
        BlockRuleResponse data = BlockRuleResponse.from(
                blockRuleService.addRule(request.childId(), request.pattern()));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** 자녀의 차단 규칙 목록(부모) — No.6. */
    @GetMapping("/rules/{childId}")
    public ResponseEntity<ApiResponse<List<BlockRuleResponse>>> getRules(@PathVariable Long childId) {
        List<BlockRuleResponse> data = blockRuleService.getRules(childId).stream()
                .map(BlockRuleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** 차단 규칙 삭제 — No.6. ([VULN-FILT-1] 참조: IDOR/하드삭제/감사부재) */
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long ruleId) {
        blockRuleService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 자녀 접속 대상 감지(자녀 앱) — No.5. 차단 대상이면 알림 발행. */
    @PostMapping("/visit")
    public ResponseEntity<ApiResponse<VisitResponse>> reportVisit(@Valid @RequestBody VisitRequest request) {
        boolean blocked = filteringService.detect(request.childId(), request.target());
        VisitResponse data = new VisitResponse(request.childId(), request.target(), blocked);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
