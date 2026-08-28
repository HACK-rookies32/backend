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
@Tag(name = "39. 관리자 - 자금 조회", description = "전체 거래 내역(필터·페이징) · 결제 통계 대시보드 — SUPER/GENERAL_ADMIN")
@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class AdminFinanceQueryController {

    private final AdminFinanceQueryService adminFinanceQueryService;

    /** No.47 전체 충전/결제 내역(필터·페이징). */
    @Operation(summary = "전체 충전/결제 내역",
            description = """
                    전체 회원의 거래 원장을 `userId`·`type`·기간 필터로 페이징 조회한다.
                    `userId` 를 지정하면 응답의 `walletBalance` 에 해당 회원의 현재 잔액이 함께 담긴다.

                    **응답 `content[]` 필드**

                    | 필드 | 설명 |
                    | --- | --- |
                    | `userId` | 거래 주체 회원 id |
                    | `userName` | 거래 주체 실명. 탈퇴 등으로 못 찾으면 `null` |
                    | `type` | 거래 유형 — 아래 표 참고 |
                    | `amount` · `balanceAfter` | 거래 금액 · 거래 후 잔액(원) |
                    | `refUid` | 거래 유형별 참조 키 — 아래 표 참고 |
                    | `merchantName` | 매장명. `PAYMENT` 계열 전용이며 **v1 미제공(`null`)** |
                    | `method` | 결제수단. 결제수단 정보를 저장하지 않아 `CHARGE` 계열은 고정 `"카드"`, 그 외 `null` |

                    **`type` 별 발생 시점과 지갑 방향**

                    | type | 발생 | 지갑 방향 |
                    | --- | --- | --- |
                    | `CHARGE` | 충전 승인 · PG 웹훅 확정 | 증가 |
                    | `REFUND` | 충전 환불(부모) / 관리자 강제환불 | 차감 / (강제환불은 자녀지갑 증가) |
                    | `TRANSFER_OUT` | 용돈 이체 — 보내는 지갑 | 차감 |
                    | `TRANSFER_IN` | 용돈 이체 — 받는 지갑 | 증가 |
                    | `PAYMENT` | QR 매장결제 승인 | 차감 |
                    | `PAYMENT_CANCEL` | 매장결제 취소 | 증가 |
                    | `ADJUST` | 관리자 잔액 수동조정 | 증가/차감(부호) |

                    ⚠️ `REFUND` 는 **충전 환불**과 **관리자 강제환불(QR 결제)** 두 사건에 모두 쓰인다.
                    화면에서 구분하려면 `refUid` 접두사(`kid_` = 충전, `qr_` = 결제)로 판단한다.

                    **`refUid` — 거래 유형별 참조 키**

                    | type | refUid |
                    | --- | --- |
                    | `CHARGE` · `REFUND`(충전 환불) | `merchantUid` |
                    | `PAYMENT` · `PAYMENT_CANCEL` | `qrToken` |
                    | `REFUND`(관리자 강제환불) | ⚠️ `qrToken` |
                    | `TRANSFER_IN` · `TRANSFER_OUT` | 상대 회원 id(문자열) |
                    | `ADJUST` | 조정 사유 텍스트 |
                    """)
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
