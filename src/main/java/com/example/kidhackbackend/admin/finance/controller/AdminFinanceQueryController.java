package com.example.kidhackbackend.admin.finance.controller;

import com.example.kidhackbackend.admin.finance.dto.AdminChargePageResponse;
import com.example.kidhackbackend.admin.finance.dto.AdminPaymentPageResponse;
import com.example.kidhackbackend.admin.finance.dto.AdminTxPageResponse;
import com.example.kidhackbackend.admin.finance.dto.AdminWalletPageResponse;
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

    /** 회원 지갑 목록 — 프론트 3차 2번(잔액 조정 진입점). */
    @Operation(summary = "회원 지갑 목록",
            description = """
                    전 회원의 **현재 잔액**을 페이징 조회한다. 「회원 지갑」 탭은 조회 화면이 아니라
                    잔액 조정(`POST /finance/users/{userId}/adjust`)의 **진입점**이라 이 목록이 있어야 대상을 고를 수 있다.
                    거래 원장(`GET /finance/transactions`)은 거래 흐름이라 회원별 현재 잔액을 대체할 수 없다.

                    **Query 파라미터**

                    | 필드 | 필수 | 설명 |
                    | --- | --- | --- |
                    | `role` | — | `PARENT` / `CHILD`. 생략 시 둘 다 |
                    | `keyword` | — | 실명(`username`) 또는 로그인 아이디 부분일치 |
                    | `page` · `size` | — | 기본 0 / 20 |

                    **응답 `content[]`**

                    | 필드 | 설명 |
                    | --- | --- |
                    | `userId` | 회원 id — 잔액 조정 API 의 path 파라미터로 그대로 쓴다 |
                    | `userName` | 회원 실명 |
                    | `role` | `PARENT` / `CHILD` |
                    | `balance` | 현재 잔액(원). **지갑이 아직 없으면 0** |
                    | `updatedAt` | 지갑 최근 변동 시각. 지갑이 없으면 `null` |

                    - **지갑이 없는 회원도 포함된다**(한 번도 충전·이체를 받지 않은 자녀). 빠지면 조정 대상으로 고를 수 없다.
                    - 관리자 계정(SUPER/GENERAL/COMMUNITY/INQUIRY_ADMIN)은 목록에서 제외한다.
                    """)
    @GetMapping("/wallets")
    public ResponseEntity<ApiResponse<AdminWalletPageResponse>> wallets(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminFinanceQueryService.getWallets(role, keyword, page, size)));
    }

    /** 매장결제 목록 — 3분할 2/3. 원장에 없는 매장명·결제 후 잔액 제공. */
    @Operation(summary = "매장결제 목록",
            description = """
                    자녀의 QR 매장결제 내역을 페이징 조회한다. **결제된 건(PAID·CANCELLED)만** 나온다
                    — 발급만 되고 안 쓴 QR, 만료된 QR 은 제외된다.

                    거래 원장(`GET /finance/transactions`)에는 **매장명이 없다**(원장에 매장 정보가 없음).
                    이 API 는 QR 결제 테이블을 직접 읽어 **실제 매장명과 결제 후 잔액**을 제공한다.

                    **Query 파라미터**

                    | 필드 | 필수 | 설명 |
                    | --- | --- | --- |
                    | `childId` | — | 특정 자녀만 |
                    | `status` | — | `PAID` / `CANCELLED` |
                    | `merchant` | — | 매장명 부분일치 |
                    | `keyword` | — | 자녀 또는 보호자 실명 부분일치 |
                    | `page` · `size` | — | 기본 0 / 20 |

                    **응답 `content[]`**

                    | 필드 | 설명 |
                    | --- | --- |
                    | `paymentId` | QR 결제 id |
                    | `childId` · `childName` | 결제한 자녀 |
                    | `parentId` · `parentName` | 보호자. 페어링이 없으면 `null` |
                    | `merchantName` | **매장명.** 단말 인증 없이 결제되면 `"UNKNOWN"` |
                    | `amount` · `balanceAfter` | 결제 금액 · 결제 직후 자녀 잔액 |
                    | `status` | `PAID` / `CANCELLED` |
                    | `paidAt` | 결제 시각 |
                    | `refundedAt` · `refundReason` | 환불 이력·사유를 저장하지 않아 **v1 은 항상 `null`** |

                    - `balanceAfter` 는 원장에서 가장 최근 `PAYMENT` 1건을 고른다 — 같은 QR 로 중복 결제가
                      가능해(의도된 취약점) 여러 건이 있을 수 있다.
                    - `merchantName` 이 `UNKNOWN` 인 건은 **단말 토큰 없이 승인된 결제**다(`[VULN-WALLET-15]`).
                    """)
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<AdminPaymentPageResponse>> payments(
            @RequestParam(required = false) Long childId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminFinanceQueryService.getPayments(childId, status, merchant, keyword, page, size)));
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

    /** #145 충전 목록 (3분할 3/3): 원장에 없는 주문 상태·PG 거래 ID·취소 사유를 제공한다. */
    @Operation(summary = "충전 목록",
            description = """
                    충전 주문(`charge_order`)을 회원 정보와 함께 페이징 조회한다. 관리자 웹 「자금·결제 관리」의
                    **충전 탭** 전용이다.

                    **왜 거래내역(`GET /finance/transactions`)으로 대체할 수 없나**

                    거래 원장은 "얼마가 오갔는가"만 남긴다. 그래서 **아직 결제되지 않은 `READY` 주문이나
                    만료된 주문은 원장에 아예 나타나지 않고**, 주문 상태·PG 거래 ID 도 알 수 없다.

                    **Query 파라미터**

                    | 파라미터 | 필수 | 설명 |
                    | --- | --- | --- |
                    | `userId` | | 주문자 회원 id. 생략 시 전체 |
                    | `status` | | 주문 상태. 생략 시 전체 |
                    | `keyword` | | 주문자 실명 또는 이메일 부분일치 |
                    | `page` · `size` | | 기본 0 / 20 |

                    **`status` 값은 6개다 — 철자 주의**

                    | 값 | 뜻 |
                    | --- | --- |
                    | `READY` | 준비 완료, PG 결제 대기 |
                    | `PAID` | 승인 완료, 잔액 적립됨 |
                    | `FAILED` | PG 가 결제 실패/취소 웹훅을 보냄 |
                    | `REFUNDED` | 결제됐다가 환불·취소됨 |
                    | `CANCELLED` | 사용자가 결제 진행 중 취소(READY 에서만 전이) |
                    | `EXPIRED` | approve 없이 방치돼 자동 만료된 READY 주문 |

                    ⚠️ `CANCELLED` 는 **L 이 2개**다. DB check 제약(`ck_charge_order_status`)에 그렇게 박혀 있다.

                    **취소 사유·시각**

                    `cancelReason`·`canceledAt` 은 **관리자 충전취소**(`POST /finance/charges/{merchantUid}/cancel`)로
                    되돌린 건만 값이 있다. 부모가 직접 환불한 건은 사유를 받지 않아 `null` 이다
                    → **이 필드의 유무로 관리자 취소와 부모 환불이 구분된다.**

                    - `method` 는 MockPG 라 결제수단을 저장하지 않아 **고정 `"카드"`** 다.
                    - `userName`·`userEmail` 은 회원이 삭제되면 `null` 이다(충전 이력은 남는다).
                    """)
    @GetMapping("/charges")
    public ResponseEntity<ApiResponse<AdminChargePageResponse>> charges(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminFinanceQueryService.getCharges(userId, status, keyword, page, size)));
    }
}
