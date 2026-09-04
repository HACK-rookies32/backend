package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 무결성 증명 challenge 응답 — 서버가 발급한 일회성 nonce.
 * 모바일은 이 nonce 로 네이티브 attestation 서명을 만들어 결제 요청에 실어 보낸다.
 */
@Schema(description = "무결성 증명 challenge (서버 발급 nonce)")
public record AttestationChallengeResponse(
        @Schema(description = "일회성 nonce(hex). TTL 내 1회만 유효.")
        String nonce) {
}
