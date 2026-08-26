package com.example.kidhackbackend.users.qrpay.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.qrpay.dto.TerminalAuthResponse;
import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;
import com.example.kidhackbackend.users.qrpay.service.StoreTerminalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매장 단말 인증 경로 — {@code /api/qrpay/terminal/**}. 앱 JWT가 아니라 {@code X-Store-Terminal-Token}
 * 헤더로 단말을 인증한다(시큐리티는 permitAll, 진위는 토큰 검증으로 담보). No.44 결제 승인이 이 경로/검증을 사용한다.
 *
 * <p>본 컨트롤러의 {@code /me}는 단말 토큰이 유효한지 확인하는 용도(발급 후 단말 프로비저닝 점검).
 */
@Tag(name = "QR Terminal", description = "매장 단말 인증 경로 — X-Store-Terminal-Token 헤더 사용")
@RestController
@RequestMapping("/api/qrpay/terminal")
@RequiredArgsConstructor
public class QrTerminalController {

    private final StoreTerminalService storeTerminalService;

    /** 단말 토큰 인증 확인: 헤더의 토큰으로 단말을 검증해 매장 정보를 돌려준다. */
    @Operation(summary = "매장 단말 인증 확인", description = "X-Store-Terminal-Token을 검증하고 인증된 매장 정보를 반환한다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TerminalAuthResponse>> me(
            @RequestHeader(value = "X-Store-Terminal-Token", required = false) String terminalToken) {
        StoreTerminal terminal = storeTerminalService.verify(terminalToken);
        return ResponseEntity.ok(ApiResponse.success(TerminalAuthResponse.from(terminal)));
    }
}
