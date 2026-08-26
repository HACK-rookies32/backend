package com.example.kidhackbackend.users.qrpay.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.qrpay.dto.QrPaymentHistoryPageResponse;
import com.example.kidhackbackend.users.qrpay.service.QrPaymentHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자녀 결제 내역 조회 API — QR결제 No.45. 부모(PARENT)/자녀(CHILD) 접근.
 *
 * <p>시큐리티는 {@code QrPaySecurityConfig} 앱 체인(@Order 10, /api/qrpay/** 인증)이 담당.
 * CHILD는 본인 내역, PARENT는 childId로 자녀 내역을 조회한다.
 */
@Tag(name = "QR Payment History", description = "자녀 결제 내역 조회 API — JWT 인증 필요(PARENT/CHILD)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/qrpay/payments")
@RequiredArgsConstructor
public class QrPaymentHistoryController {

    private final QrPaymentHistoryService qrPaymentHistoryService;

    private void requireParentOrChild(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.PARENT && member.getRole() != Role.CHILD) {
            throw new IllegalArgumentException("부모 또는 자녀 회원만 이용할 수 있는 기능입니다.");
        }
    }

    /** 자녀 결제 내역 페이징 조회 — No.45. */
    @Operation(summary = "자녀 결제 내역 조회", description = "결제된(PAID/CANCELLED) QR 내역을 매장명·금액·시각과 함께 페이징 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<QrPaymentHistoryPageResponse>> payments(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false) Long childId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireParentOrChild(member);
        return ResponseEntity.ok(ApiResponse.success(
                qrPaymentHistoryService.getPayments(member, childId, page, size)));
    }
}
