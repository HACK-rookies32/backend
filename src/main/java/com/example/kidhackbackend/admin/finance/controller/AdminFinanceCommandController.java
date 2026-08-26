package com.example.kidhackbackend.admin.finance.controller;

import com.example.kidhackbackend.admin.finance.dto.AdjustRequest;
import com.example.kidhackbackend.admin.finance.dto.AdminActionResponse;
import com.example.kidhackbackend.admin.finance.service.AdminFinanceCommandService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 자금 변경 API — 역할표 No.48(강제환불)·No.50(잔액조정). <b>SUPER_ADMIN 전용이어야 한다.</b>
 *
 * <p>시큐리티는 {@code AdminFinanceSecurityConfig}(/api/admin/finance/**, SUPER+GENERAL_ADMIN)가 담당.
 * ⚠️ 이 2종은 SUPER_ADMIN 전용이어야 하나 체인이 GENERAL_ADMIN도 허용하고 컨트롤러가 역할을 재확인하지 않는다
 * → 일반관리자(GENERAL_ADMIN)도 강제환불·잔액조정을 호출할 수 있다({@code [VULN-ADMIN-1]} 권한경계/BFLA).
 * 방어: SUPER_ADMIN 재확인(체인 분리 또는 컨트롤러 requireSuperAdmin).
 */
@Tag(name = "Admin Finance Command", description = "관리자 자금 변경 API — SUPER_ADMIN(의도)")
@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class AdminFinanceCommandController {

    private final AdminFinanceCommandService adminFinanceCommandService;

    /** No.48 강제환불: 자녀 QR 결제를 강제 환불. */
    @Operation(summary = "관리자 강제환불", description = "자녀 QR 결제를 강제 환불하고 자녀 지갑을 원복한다.")
    @PostMapping("/payments/{qrToken}/force-refund")
    public ResponseEntity<ApiResponse<AdminActionResponse>> forceRefund(@PathVariable String qrToken) {
        return ResponseEntity.ok(ApiResponse.success(adminFinanceCommandService.forceRefund(qrToken)));
    }

    /** No.50 잔액 수동조정: 특정 회원 잔액 가감. */
    @Operation(summary = "잔액 수동조정", description = "특정 회원 잔액을 사유와 함께 가감한다(ADJUST).")
    @PostMapping("/users/{userId}/adjust")
    public ResponseEntity<ApiResponse<AdminActionResponse>> adjust(
            @PathVariable Long userId,
            @RequestBody AdjustRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminFinanceCommandService.adjust(userId, request.amount(), request.reason())));
    }
}
