package com.example.kidhackbackend.users.efin.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.efin.dto.OtpIssueResponse;
import com.example.kidhackbackend.users.efin.dto.OtpVerifyRequest;
import com.example.kidhackbackend.users.efin.dto.OtpVerifyResponse;
import com.example.kidhackbackend.users.efin.service.EfinOtpService;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전자금융 거래 2차 인증(OTP/ARS) — 거래 인증수단 검증. 로그인 사용자 본인 전화번호로 OTP 발급·확인.
 */
@Tag(name = "20. 전자금융 - 거래 OTP", description = "거래 2차 인증(OTP/ARS mock). 송금·회수 전 본인 전화번호로 인증번호 발급·확인.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/efin/otp")
@RequiredArgsConstructor
public class EfinOtpController {

    private final EfinOtpService otpService;

    @Operation(summary = "거래 OTP 발급",
            description = """
                    로그인 사용자 본인 전화번호로 거래 인증번호를 발급하고 SMS/ARS(mock)로 발신한다.

                    ⚠️ [VULN-EFIN-1] 발급된 인증번호를 응답 body(`code`)에 그대로 노출한다(mock 발신, 응답만 가로채면 OTP 획득 — VULN-AUTH-MAIL 동형).""")
    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<OtpIssueResponse>> issue(@AuthenticationPrincipal Member member) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(otpService.issue(member)));
    }

    @Operation(summary = "거래 OTP 확인",
            description = """
                    발급된 인증번호가 유효한지 확인한다(송금 전 UI 점검용 — 소비하지 않음).

                    ⚠️ [VULN-EFIN-1b] 실패 횟수 제한이 없어 6자리(10^6) 무제한 시도로 뚫린다(로그인 VULN-4 계열).""")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> verify(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody OtpVerifyRequest request) {
        requireLogin(member);
        boolean valid = otpService.verify(member, request.code());
        return ResponseEntity.ok(ApiResponse.success(new OtpVerifyResponse(valid)));
    }

    private void requireLogin(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
    }
}