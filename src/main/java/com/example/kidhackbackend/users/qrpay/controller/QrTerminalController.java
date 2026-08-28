package com.example.kidhackbackend.users.qrpay.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.qrpay.dto.QrApproveRequest;
import com.example.kidhackbackend.users.qrpay.dto.QrApproveResponse;
import com.example.kidhackbackend.users.qrpay.dto.TerminalAuthResponse;
import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;
import com.example.kidhackbackend.users.qrpay.service.QrApprovalService;
import com.example.kidhackbackend.users.qrpay.service.StoreTerminalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매장 단말 인증 경로 — {@code /api/qrpay/terminal/**}. 앱 JWT가 아니라 {@code X-Store-Terminal-Token}
 * 헤더로 단말을 인증한다(시큐리티는 permitAll, 진위는 토큰 검증으로 담보). No.44 결제 승인이 이 경로/검증을 사용한다.
 *
 * <p>본 컨트롤러의 {@code /me}는 단말 토큰이 유효한지 확인하는 용도(발급 후 단말 프로비저닝 점검).
 */
@Tag(name = "16. QR결제 - 매장 단말", description = "매장 단말이 QR을 스캔해 결제 승인 · 단말 토큰 확인 — X-Store-Terminal-Token 헤더")
@RestController
@RequestMapping("/api/qrpay/terminal")
@RequiredArgsConstructor
public class QrTerminalController {

    private final StoreTerminalService storeTerminalService;
    private final QrApprovalService qrApprovalService;

    /** 단말 토큰 인증 확인: 헤더의 토큰으로 단말을 검증해 매장 정보를 돌려준다. */
    @Operation(summary = "매장 단말 인증 확인", description = "X-Store-Terminal-Token을 검증하고 인증된 매장 정보를 반환한다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TerminalAuthResponse>> me(
            @RequestHeader(value = "X-Store-Terminal-Token", required = false) String terminalToken) {
        StoreTerminal terminal = storeTerminalService.verify(terminalToken);
        return ResponseEntity.ok(ApiResponse.success(TerminalAuthResponse.from(terminal)));
    }

    /** QR 스캔 결제 승인 — No.44. 단말이 스캔한 QR과 금액으로 결제를 확정한다. */
    @Operation(summary = "QR 스캔 결제 승인", description = "매장 단말이 자녀 QR을 스캔해 결제를 승인한다(자녀 잔액 차감 + 부모 알림).")
    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<QrApproveResponse>> pay(
            @RequestHeader(value = "X-Store-Terminal-Token", required = false) String terminalToken,
            @Valid @RequestBody QrApproveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                qrApprovalService.approve(terminalToken, request.qrToken(), request.amount())));
    }
}
