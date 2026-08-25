package com.example.kidhackbackend.child.controller;

import com.example.kidhackbackend.child.dto.ChildDashboardResponse;
import com.example.kidhackbackend.child.service.ChildDashboardService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /** 자녀 관리 대시보드: 현재 위치·실시간 상태·최근 알림·차단목록·등록공간을 한 번에 조회(부모). */
    // [VULN-CHILD-1] 경로 childId 소유권 미검증 → 남의 자녀 대시보드 전체 열람(IDOR).
    @GetMapping("/{childId}/dashboard")
    public ResponseEntity<ApiResponse<ChildDashboardResponse>> dashboard(@PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(childDashboardService.getDashboard(childId)));
    }
}
