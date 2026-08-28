package com.example.kidhackbackend.users.wallet.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.wallet.dto.ChargeApproveRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargeApproveResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargeHistoryPageResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargeOrderPageResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargeOrderResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareResponse;
import com.example.kidhackbackend.users.wallet.dto.RefundResponse;
import com.example.kidhackbackend.users.wallet.dto.TxPageResponse;
import com.example.kidhackbackend.users.wallet.service.WalletChargeService;
import com.example.kidhackbackend.users.wallet.service.WalletHistoryService;
import com.example.kidhackbackend.users.wallet.service.WalletRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지갑 충전 API — 역할표 No.35 (PG Mock 준비/승인 2단계). 부모(PARENT) 전용.
 *
 * <p>흐름: {@code prepare}(READY 주문 + merchantUid 발급) → 앱이 PG 결제(mock 이면 생략) → {@code approve}(승인 + 잔액 적립).
 * 결제 진행 중 취소는 {@code cancel}, 이미 결제된 건의 환불은 {@code refund}. PG 서버 웹훅 확정(No.36)은 태그 14 참고.
 *
 * <p>⚠️ 이 도메인은 자금 조작 모의해킹의 핵심이라 {@code [VULN-WALLET-1~6]} 취약점이 의도적으로 심어져 있다.
 * (단 [VULN-WALLET-6] 의 "잔액 음수" 부분은 R-2 로 막았고, 소유권 미검증만 남아 있다.)
 */
@Tag(name = "11. 지갑 - 충전/환불",
        description = "지갑 충전 준비/승인(2단계) · 결제 취소 · 원장/주문/통합 내역(페이징) · 환불 — PARENT 전용. "
                + "⚠️ [VULN-WALLET-1~6] 의도적 포함")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletChargeService walletChargeService;
    private final WalletHistoryService walletHistoryService;
    private final WalletRefundService walletRefundService;

    private void requireParent(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.PARENT) {
            throw new IllegalArgumentException("부모 회원만 이용할 수 있는 기능입니다.");
        }
    }

    /** 충전 1단계 — 준비: READY 주문 생성 후 merchantUid + 결제창 정보 발급. */
    @Operation(summary = "충전 준비 (1단계)",
            description = """
                    READY 주문을 만들고 merchantUid 와 결제창 정보를 발급한다. 응답의 `mock=true` 면 결제창 없이 곧바로 approve 를 호출.

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `amount` | ✅ | 충전 금액(원, 1 이상) |
                    """)
    @PostMapping("/charge/prepare")
    public ResponseEntity<ApiResponse<ChargePrepareResponse>> prepare(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody ChargePrepareRequest request) {
        requireParent(member);
        return ResponseEntity.ok(
                ApiResponse.success(walletChargeService.prepare(member.getId(), request)));
    }

    /** 결제 진행 중 취소 — READY 주문만 CANCELLED (잔액 변화 없음). */
    @Operation(summary = "결제 취소",
            description = "승인 전(READY) 주문을 취소한다. 이미 결제(PAID)된 주문은 취소 불가(환불 이용). "
                    + "이 API 는 소유권을 검증한다(본인 주문만).")
    @PostMapping("/charge/{merchantUid}/cancel")
    public ResponseEntity<ApiResponse<ChargeOrderResponse>> cancel(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "취소할 충전 주문번호(prepare 가 발급한 merchantUid)") @PathVariable String merchantUid) {
        requireParent(member);
        return ResponseEntity.ok(
                ApiResponse.success(walletChargeService.cancel(member.getId(), merchantUid)));
    }

    /** 충전 2단계 — 승인: PG 승인 확인 후 주문 확정 및 잔액 적립. */
    @Operation(summary = "충전 승인 (2단계)",
            description = """
                    merchantUid 로 준비 주문을 찾아(없으면 404) PG 승인을 확인하고 잔액을 적립한다.

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `merchantUid` | ✅ | 준비 단계에서 받은 가맹점 주문번호 |
                    | `amount` | ✅ | 결제 금액(원, 1 이상) |
                    | `pgTransactionId` | — | PG 거래 ID (mock 환경에서는 생략 가능) |

                    ⚠️ [VULN-WALLET-1] 적립액이 order.amount 가 아니라 요청 body 의 amount → 금액 변조. [VULN-WALLET-2] 주문 소유권 미검증(BOLA). [VULN-WALLET-3] 멱등성/락 없음 → N번 호출 시 N배 적립.""")
    @PostMapping("/charge/approve")
    public ResponseEntity<ApiResponse<ChargeApproveResponse>> approve(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody ChargeApproveRequest request) {
        requireParent(member);
        return ResponseEntity.ok(
                ApiResponse.success(walletChargeService.approve(member.getId(), request)));
    }

    /** 충전내역: 본인 지갑 원장(충전/환불) 목록 — No.37. */
    @Operation(summary = "원장 내역 조회 (페이징)",
            description = "본인 지갑의 거래 원장(CHARGE/REFUND/…) 을 최신순 페이징으로 조회한다.")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<TxPageResponse>> transactions(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletHistoryService.getTransactions(member.getId(), PageRequest.of(page, size))));
    }

    /** 충전내역: 본인 충전 주문(상태 포함) 목록 — No.37. */
    @Operation(summary = "충전 주문 내역 조회 (페이징)",
            description = "본인 충전 주문을 상태(READY/PAID/FAILED/REFUNDED/CANCELLED/EXPIRED)와 함께 최신순 페이징으로 조회한다.")
    @GetMapping("/charges")
    public ResponseEntity<ApiResponse<ChargeOrderPageResponse>> charges(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletHistoryService.getCharges(member.getId(), PageRequest.of(page, size))));
    }

    /** 충전 내역 통합 뷰 — 주문 + 환불 시각을 한 줄로 (No.37, P-3). */
    @Operation(summary = "충전 내역 통합 조회 (페이징)",
            description = "충전 주문 정보에 환불 시각을 합쳐 한 번에 내려준다. 프론트가 주문/원장을 따로 조합할 필요가 없다.")
    @GetMapping("/charge-history")
    public ResponseEntity<ApiResponse<ChargeHistoryPageResponse>> chargeHistory(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletHistoryService.getChargeHistory(member.getId(), PageRequest.of(page, size))));
    }

    /** 취소·환불: 완료된 충전을 전액 환불 — No.37. */
    @Operation(summary = "충전 환불 (전액)",
            description = "merchantUid 의 충전 주문을 찾아(없으면 404) 전액 환불한다(잔액 차감 · REFUND 원장 · 주문 REFUNDED). "
                    + "환불액이 현재 잔액보다 크면 거부(400, 잔액 음수 방지). 부분 환불은 미지원. "
                    + "⚠️ [VULN-WALLET-5] 소유권 미검증 → 남의 충전을 환불시켜 타인 지갑 차감 가능. "
                    + "[VULN-WALLET-6] 상태 미검증(잔액 음수는 R-2 로 차단됨).")
    @PostMapping("/charge/{merchantUid}/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "환불할 충전 주문번호(prepare 가 발급한 merchantUid)") @PathVariable String merchantUid) {
        requireParent(member);
        return ResponseEntity.ok(
                ApiResponse.success(walletRefundService.refund(member.getId(), merchantUid)));
    }
}
