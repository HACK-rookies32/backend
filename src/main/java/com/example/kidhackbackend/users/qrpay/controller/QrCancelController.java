package com.example.kidhackbackend.users.qrpay.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.qrpay.dto.QrCancelResponse;
import com.example.kidhackbackend.users.qrpay.service.QrCancelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매장결제 취소 API — QR결제 No.46. 부모(PARENT) 전용.
 *
 * <p>시큐리티는 {@code QrPaySecurityConfig} 앱 체인(@Order 10, /api/qrpay/** 인증)이 담당.
 * 본인 자녀 결제만 취소해야 하므로(정상) 컨트롤러에서 PARENT 역할을 확인한다.
 */
@Tag(name = "QR Payment Cancel", description = "매장결제 취소 API — JWT 인증 필요(PARENT)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/qrpay/payments")
@RequiredArgsConstructor
public class QrCancelController {

    private final QrCancelService qrCancelService;

    private void requireParent(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.PARENT) {
            throw new IllegalArgumentException("부모 회원만 결제를 취소할 수 있습니다.");
        }
    }

    /** 매장결제 취소: 자녀 결제를 취소하고 자녀 잔액을 환급 — No.46. */
    @Operation(summary = "매장결제 취소", description = "qrToken의 자녀 결제를 취소하고 자녀 지갑에 전액 환급한다(부모 알림).")
    @PostMapping("/{qrToken}/cancel")
    public ResponseEntity<ApiResponse<QrCancelResponse>> cancel(
            @AuthenticationPrincipal Member member,
            @PathVariable String qrToken) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(qrCancelService.cancel(member.getId(), qrToken)));
    }
}
