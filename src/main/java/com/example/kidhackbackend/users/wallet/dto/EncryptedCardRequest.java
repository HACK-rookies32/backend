package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 카드 등록 요청 — 하이브리드 E2E 암호화 blob(규약 확정본).
 *
 * <p>프론트가 카드 payload(JSON)를 AES-GCM으로 암호화하고, 그 AES키를 서버 공개키로 RSA-OAEP 암호화해
 * 담는다. 서버는 {@code keyId}로 개인키를 골라 복호한다.
 */
public record EncryptedCardRequest(
        @Schema(description = "공개키 식별자(GET /cards/public-key 로 받은 값)", example = "card-key-1a2b3c")
        @NotBlank(message = "keyId는 필수입니다.") String keyId,

        @Schema(description = "RSA-OAEP(SHA-256)로 암호화한 1회용 AES키, Base64")
        @NotBlank(message = "encryptedKey는 필수입니다.") String encryptedKey,

        @Schema(description = "AES-GCM IV(12바이트), Base64")
        @NotBlank(message = "iv는 필수입니다.") String iv,

        @Schema(description = "AES-GCM으로 암호화한 카드 payload(JSON), Base64")
        @NotBlank(message = "ciphertext는 필수입니다.") String ciphertext) {
}
