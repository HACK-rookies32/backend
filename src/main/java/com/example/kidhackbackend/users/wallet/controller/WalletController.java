package com.example.kidhackbackend.users.wallet.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.wallet.dto.ChargeApproveRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargeApproveResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargeOrderResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareRequest;
import com.example.kidhackbackend.users.wallet.dto.ChargePrepareResponse;
import com.example.kidhackbackend.users.wallet.dto.RefundResponse;
import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.service.WalletChargeService;
import com.example.kidhackbackend.users.wallet.service.WalletHistoryService;
import com.example.kidhackbackend.users.wallet.service.WalletRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지갑 충전 API — 역할표 No.35 (PG Mock 준비/승인 2단계). 부모(PARENT) 전용.
 */
@Tag(name = "Wallet", description = "지갑 충전 API — JWT 인증 필요(PARENT)")
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

    /** 충전 1단계 — 준비: READY 주문 생성 후 merchantUid 발급. */
    @Operation(summary = "충전 준비", description = "충전 금액으로 결제 주문을 만들고 merchantUid를 발급한다.")
    @PostMapping("/charge/prepare")
    public ResponseEntity<ApiResponse<ChargePrepareResponse>> prepare(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody ChargePrepareRequest request) {
        requireParent(member);
        return ResponseEntity.ok(
                ApiResponse.success(walletChargeService.prepare(member.getId(), request)));
    }

    /** 충전 2단계 — 승인: PG 승인 확인 후 주문 확정 및 잔액 적립. */
    @Operation(summary = "충전 승인", description = "merchantUid로 PG 승인을 확인하고 잔액을 적립한다.")
    @PostMapping("/charge/approve")
    public ResponseEntity<ApiResponse<ChargeApproveResponse>> approve(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody ChargeApproveRequest request) {
        requireParent(member);
        return ResponseEntity.ok(
                ApiResponse.success(walletChargeService.approve(member.getId(), request)));
    }

    /** 충전내역: 본인 지갑 원장(충전/환불) 목록 — No.37. */
    @Operation(summary = "원장 내역 조회", description = "본인 지갑의 충전/환불 원장을 최신순으로 조회한다.")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TxResponse>>> transactions(
            @AuthenticationPrincipal Member member) {
        requireParent(member);
        return ResponseEntity.ok(
                ApiResponse.success(walletHistoryService.getTransactions(member.getId())));
    }

    /** 충전내역: 본인 충전 주문(상태 포함) 목록 — No.37. */
    @Operation(summary = "충전 주문 내역 조회", description = "본인 충전 주문을 상태와 함께 최신순으로 조회한다.")
    @GetMapping("/charges")
    public ResponseEntity<ApiResponse<List<ChargeOrderResponse>>> charges(
            @AuthenticationPrincipal Member member) {
        requireParent(member);
        return ResponseEntity.ok(
                ApiResponse.success(walletHistoryService.getCharges(member.getId())));
    }

    /** 취소·환불: 완료된 충전을 전액 환불 — No.37. */
    @Operation(summary = "충전 환불", description = "merchantUid의 충전을 전액 환불(잔액 차감·주문 취소)한다.")
    @PostMapping("/charge/{merchantUid}/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @AuthenticationPrincipal Member member,
            @PathVariable String merchantUid) {
        requireParent(member);
        return ResponseEntity.ok(
                ApiResponse.success(walletRefundService.refund(member.getId(), merchantUid)));
    }
}
