package com.example.kidhackbackend.admin.finance.controller;

import com.example.kidhackbackend.admin.finance.dto.AdjustRequest;
import com.example.kidhackbackend.admin.finance.dto.AdminActionResponse;
import com.example.kidhackbackend.admin.finance.dto.ChargeCancelRequest;
import com.example.kidhackbackend.admin.finance.service.AdminFinanceCommandService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.wallet.dto.RefundResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "38. 관리자 - 자금 변경", description = "강제 환불 · 잔액 수동 조정 · 충전 취소 (SUPER_ADMIN 의도 — 실제 체인은 GENERAL_ADMIN도 허용)")
@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class AdminFinanceCommandController {

    private final AdminFinanceCommandService adminFinanceCommandService;

    /** No.48 강제환불: 자녀 QR 결제를 강제 환불. */
    @Operation(summary = "관리자 강제환불",
            description = """
                    자녀 QR 결제를 강제 환불하고 자녀 지갑을 원복한다(감사 로그 `PAYMENT_REFUND` 기록).

                    **요청 필드** — 없음 (경로의 `qrToken` 으로 대상 결제를 지정한다)

                    - 존재하지 않는 `qrToken`: `404 NOT_FOUND`.
                    - ⚠️ 의도적 취약점: 이미 환불된 건도 상태 확인 없이 다시 환불된다(`[VULN-ADMIN-3]`).
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "강제환불 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 결제",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "data": null,
                             "error": {"code": "NOT_FOUND", "message": "결제를 찾을 수 없습니다."}}""")))
    })
    @PostMapping("/payments/{qrToken}/force-refund")
    public ResponseEntity<ApiResponse<AdminActionResponse>> forceRefund(@PathVariable String qrToken) {
        return ResponseEntity.ok(ApiResponse.success(adminFinanceCommandService.forceRefund(qrToken)));
    }

    /** No.50 잔액 수동조정: 특정 회원 잔액 가감. */
    @Operation(summary = "잔액 수동조정",
            description = """
                    특정 회원 잔액을 사유와 함께 가감한다(`ADJUST` 원장 + 감사 로그 `WALLET_BALANCE_ADJUST` 기록).

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    | --- | --- | --- |
                    | `amount` | ✅ | 가감액(원). 음수면 차감 |
                    | `reason` | ✅ | 조정 사유 — 감사 로그에 그대로 남는다 |

                    - ⚠️ 의도적 취약점: 사유가 비어 있어도, 금액 한도가 없어도 그대로 통과한다(`[VULN-ADMIN-2]`).
                    """)
    @PostMapping("/users/{userId}/adjust")
    public ResponseEntity<ApiResponse<AdminActionResponse>> adjust(
            @PathVariable Long userId,
            @RequestBody AdjustRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminFinanceCommandService.adjust(userId, request.amount(), request.reason())));
    }

    /** #139 충전 취소: 완료된 충전을 관리자 권한으로 되돌린다(프론트 3차 1번). */
    @Operation(summary = "관리자 충전 취소",
            description = """
                    완료된 충전(`PAID`)을 관리자 권한으로 되돌린다. 회원 지갑에서 충전액을 차감하고
                    `REFUND` 원장을 남긴 뒤, 감사 로그 `CHARGE_CANCEL` 에 취소 사유를 함께 기록한다.

                    **경로 파라미터** — `merchantUid` 는 거래내역 `CHARGE` 행의 `refUid` 값(`kid_` 접두사)을
                    그대로 넣으면 된다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    | --- | --- | --- |
                    | `reason` | ✅ | 취소 사유. 공백이면 400. 감사 로그 `description` 에 그대로 남는다 |

                    **응답** — 부모용 충전 환불(`POST /api/wallet/charge/{merchantUid}/refund`)과 같은
                    `RefundResponse` 형태라 프론트가 그대로 재사용할 수 있다.

                    - 이미 취소·환불됐거나 결제되지 않은 주문(`PAID` 아님): `409 CONFLICT`.
                    - 이미 써버려 잔액이 부족한 경우: `409 CONFLICT` (부모용과 동일 정책).
                    - ⚠️ 의도적 취약점: 이 API 는 `SUPER_ADMIN` 전용이어야 하나 컨트롤러가 역할을
                      재확인하지 않아 `GENERAL_ADMIN` 도 호출할 수 있다(`[VULN-ADMIN-1]`).
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "충전 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "취소 사유 누락·공백",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "data": null,
                             "error": {"code": "INVALID_PARAMETER", "message": "reason: 취소 사유는 필수입니다."}}"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 충전 주문",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "data": null,
                             "error": {"code": "NOT_FOUND", "message": "충전 주문을 찾을 수 없습니다."}}"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리된 충전이거나 잔액 부족",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "data": null,
                             "error": {"code": "CONFLICT", "message": "환불 가능 잔액이 부족합니다. 현재 잔액: 0, 환불액: 50000"}}""")))
    })
    @PostMapping("/charges/{merchantUid}/cancel")
    public ResponseEntity<ApiResponse<RefundResponse>> cancelCharge(
            @PathVariable String merchantUid,
            @Valid @RequestBody ChargeCancelRequest request) {
        // [VULN-ADMIN-1] 권한경계 미재확인(BFLA) — 프론트 요청서는 SUPER_ADMIN 전용이라고 명시했지만
        //   시큐리티 체인(AdminFinanceSecurityConfig)이 GENERAL_ADMIN 도 허용하고 여기서 역할을 다시
        //   확인하지 않는다. → 일반관리자도 남의 충전을 취소해 지갑을 차감할 수 있다.
        //   방어: 체인 분리 또는 컨트롤러에서 SUPER_ADMIN 재확인.
        return ResponseEntity.ok(ApiResponse.success(
                adminFinanceCommandService.cancelCharge(merchantUid, request.reason())));
    }
}
