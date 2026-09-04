package com.example.kidhackbackend.users.efin.controller;

import com.example.kidhackbackend.global.crypto.AttestationNonceStore;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.efin.dto.AttestationChallengeResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모바일 네이티브 무결성 증명(attestation) — challenge 발급.
 *
 * <p>결제 직전 모바일이 nonce 를 요청하면 서버가 일회성 nonce 를 발급한다. 모바일 네이티브(.so)는
 * 이 nonce 와 기기 보안상태를 공유 비밀키로 HMAC 서명해 결제 요청 헤더(X-Attestation)에 싣고,
 * 서버는 결제 관문(QrTerminalController.pay)에서 이를 검증한다. 로그인 본인만 challenge 를 받는다.
 */
@Tag(name = "23. 전자금융 - 무결성 증명(attestation)",
        description = "모바일 네이티브 무결성 증명용 challenge nonce 발급. 결제 관문에서 X-Attestation 헤더로 검증된다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/efin/attestation")
@RequiredArgsConstructor
public class AttestationController {

    private final AttestationNonceStore nonceStore;

    @Operation(summary = "무결성 증명 challenge 발급",
            description = "일회성 nonce 를 발급한다(TTL 내 1회 유효). 모바일이 이 nonce 로 네이티브 attestation 서명을 만든다.")
    @PostMapping("/challenge")
    public ResponseEntity<ApiResponse<AttestationChallengeResponse>> challenge(
            @AuthenticationPrincipal Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return ResponseEntity.ok(ApiResponse.success(
                new AttestationChallengeResponse(nonceStore.issue())));
    }
}
