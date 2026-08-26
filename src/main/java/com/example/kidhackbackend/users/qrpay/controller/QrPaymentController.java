package com.example.kidhackbackend.users.qrpay.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.qrpay.dto.QrIssueResponse;
import com.example.kidhackbackend.users.qrpay.dto.QrStatusResponse;
import com.example.kidhackbackend.users.qrpay.service.QrPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 QR 발급/조회 API — QR결제 No.42(발급)·No.43(상태 폴링). 자녀(CHILD) 전용.
 *
 * <p>시큐리티는 {@code QrPaySecurityConfig} 앱 체인(@Order 10, /api/qrpay/** 인증)이 담당.
 * 발급 본인의 지갑으로 결제되는 QR이므로 컨트롤러에서 CHILD 역할을 확인한다.
 */
@Tag(name = "QR Payment", description = "결제 QR 발급/조회 API — JWT 인증 필요(CHILD)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/qrpay/qr")
@RequiredArgsConstructor
public class QrPaymentController {

    private final QrPaymentService qrPaymentService;

    private void requireChild(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.CHILD) {
            throw new IllegalArgumentException("자녀 회원만 결제 QR을 발급할 수 있습니다.");
        }
    }

    /** 결제 QR 발급: 1회성 토큰 + 60초 만료 — No.42. */
    @Operation(summary = "결제 QR 발급", description = "자녀가 매장 결제용 1회성 QR 토큰을 발급한다(60초 만료).")
    @PostMapping
    public ResponseEntity<ApiResponse<QrIssueResponse>> issue(
            @AuthenticationPrincipal Member member) {
        requireChild(member);
        return ResponseEntity.ok(ApiResponse.success(qrPaymentService.issue(member.getId())));
    }

    /** 결제 QR 상태 조회(폴링) — No.43. 발급 본인만 조회(의도). */
    @Operation(summary = "결제 QR 상태 조회", description = "qrToken으로 QR 상태(ISSUED/PAID/EXPIRED/CANCELLED)를 조회한다.")
    @GetMapping("/{qrToken}")
    public ResponseEntity<ApiResponse<QrStatusResponse>> status(
            @AuthenticationPrincipal Member member,
            @PathVariable String qrToken) {
        requireChild(member);
        return ResponseEntity.ok(ApiResponse.success(qrPaymentService.getStatus(member.getId(), qrToken)));
    }
}
