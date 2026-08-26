package com.example.kidhackbackend.users.child.controller;

import com.example.kidhackbackend.users.child.dto.ChildDashboardResponse;
import com.example.kidhackbackend.users.child.dto.ChildDetailResponse;
import com.example.kidhackbackend.users.child.dto.ChildSummaryResponse;
import com.example.kidhackbackend.users.child.dto.ChildUpdateRequest;
import com.example.kidhackbackend.users.child.service.ChildDashboardService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.child.service.ChildService;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 자녀 관리 페이지 API — 역할표 No.18 (통합 조회).
 */
@Tag(name = "Child", description = "자녀 관리 페이지 통합 조회 API — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/children")
@RequiredArgsConstructor
public class ChildController {

    private final ChildDashboardService childDashboardService;
    private final ChildService childService;

    /** 자녀 관리 대시보드: 현재 위치·실시간 상태·최근 알림·차단목록·등록공간을 한 번에 조회(부모). */
    // [VULN-CHILD-1] 경로 childId 소유권 미검증 → 남의 자녀 대시보드 전체 열람(IDOR).
    @GetMapping("/{childId}/dashboard")
    public ResponseEntity<ApiResponse<ChildDashboardResponse>> dashboard(@PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(childDashboardService.getDashboard(childId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChildSummaryResponse>>> getChildren(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ApiResponse.success(childService.getChildren(member.getId())));
    }

    @GetMapping("/{childId}")
    public ResponseEntity<ApiResponse<ChildDetailResponse>> getChild(@PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(childService.getChild(childId)));
    }

    @PatchMapping("/{childId}")
    public ResponseEntity<ApiResponse<ChildDetailResponse>> updateChild(@PathVariable Long childId,
                                                                        @RequestBody ChildUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(childService.updateChild(childId, request)));
    }

    @DeleteMapping("/{childId}")
    public ResponseEntity<ApiResponse<Void>> unpairChild(@PathVariable Long childId) {
        childService.unpairChild(childId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
