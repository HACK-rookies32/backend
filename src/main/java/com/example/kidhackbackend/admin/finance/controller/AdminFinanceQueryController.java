package com.example.kidhackbackend.admin.finance.controller;

import com.example.kidhackbackend.admin.finance.dto.AdminTxPageResponse;
import com.example.kidhackbackend.admin.finance.dto.AdminUserWalletResponse;
import com.example.kidhackbackend.admin.finance.dto.FinanceStatsResponse;
import com.example.kidhackbackend.admin.finance.service.AdminFinanceQueryService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 자금 조회 API — 역할표 No.47·49·51. 관리자(SUPER_ADMIN·GENERAL_ADMIN) 전용.
 *
 * <p>권한은 {@code AdminFinanceSecurityConfig}(/api/admin/finance/**, @Order 11)가 담당.
 */
@Tag(name = "Admin Finance View", description = "관리자 자금 조회 API — SUPER/GENERAL_ADMIN")
@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class AdminFinanceQueryController {

    private final AdminFinanceQueryService adminFinanceQueryService;

    /** No.47 전체 충전/결제 내역(필터·페이징). */
    @Operation(summary = "전체 충전/결제 내역", description = "전체 회원의 거래 원장을 userId·type·기간 필터로 페이징 조회한다.")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<AdminTxPageResponse>> transactions(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) TxType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminFinanceQueryService.getTransactions(userId, type, from, to, page, size)));
    }

    /** No.51 결제 통계 대시보드(DAY/MERCHANT). */
    @Operation(summary = "결제 통계 대시보드", description = "기간별 groupBy(DAY/MERCHANT)로 충전액·결제액·건수를 집계한다.")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<FinanceStatsResponse>> stats(
            @RequestParam(defaultValue = "DAY") String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(adminFinanceQueryService.getStats(groupBy, from, to)));
    }
}
