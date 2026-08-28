package com.example.kidhackbackend.users.wallet.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.wallet.dto.ChildWalletResponse;
import com.example.kidhackbackend.users.wallet.dto.MyWalletResponse;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.service.WalletViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지갑 조회 API — 내 지갑(No.38, PARENT/CHILD)·자녀 지갑(No.41, PARENT).
 *
 * <p>시큐리티: {@code WalletSecurityConfig}에서 {@code /api/wallet/me}는 PARENT+CHILD, 그 외 wallet 경로는 PARENT.
 */
@Tag(name = "13. 지갑 - 조회", description = "내 지갑 잔액/원장(PARENT·CHILD) · 자녀 지갑 조회(PARENT)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletViewController {

    private final WalletViewService walletViewService;

    private void requireLogin(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
    }

    private void requireParent(Member member) {
        requireLogin(member);
        if (member.getRole() != Role.PARENT) {
            throw new IllegalArgumentException("부모 회원만 이용할 수 있는 기능입니다.");
        }
    }

    /** 내 지갑 조회(잔액 + 필터·페이징 원장) — No.38. 본인(PARENT/CHILD). */
    @Operation(summary = "내 지갑 조회", description = "본인 지갑 잔액과 type/기간 필터·페이징된 거래 원장을 조회한다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyWalletResponse>> myWallet(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false) TxType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletViewService.getMyWallet(member.getId(), type, from, to, page, size)));
    }

    /** 자녀 지갑 조회(잔액 + 최근거래 요약) — No.41. 부모 전용. */
    @Operation(summary = "자녀 지갑 조회", description = "부모가 자녀 지갑 잔액과 최근거래 요약을 조회한다(본인 자녀만).")
    @GetMapping("/children/{childId}")
    public ResponseEntity<ApiResponse<ChildWalletResponse>> childWallet(
            @AuthenticationPrincipal Member member,
            @PathVariable Long childId) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletViewService.getChildWallet(member.getId(), childId)));
    }
}
