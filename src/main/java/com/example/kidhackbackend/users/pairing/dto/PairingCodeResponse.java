package com.example.kidhackbackend.users.pairing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "페어링 코드")
public record PairingCodeResponse(
        @Schema(description = "6자리 페어링 코드", example = "840975") String code,
        @Schema(description = "코드 만료 시각(발급 + 5분)") LocalDateTime expiresAt
) {
}
