package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 공개키 응답 — 프론트가 RSA-OAEP 암호화에 사용한다. 응답에 keyId를 동봉(회전 대비).
 */
public record CardPublicKeyResponse(
        @Schema(description = "공개키 식별자", example = "card-key-1a2b3c") String keyId,
        @Schema(description = "RSA 공개키(X.509 DER, Base64)") String publicKey,
        @Schema(description = "키 회전 주기(일)", example = "90") long keyRotationDays) {
}
