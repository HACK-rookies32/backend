package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;
import io.swagger.v3.oas.annotations.media.Schema;

/** 매장 단말 인증 확인 응답 (X-Store-Terminal-Token 검증 결과). 토큰 값은 담지 않는다. */
@Schema(description = "매장 단말 인증 결과")
public record TerminalAuthResponse(
        @Schema(description = "단말 ID", example = "1") Long terminalId,
        @Schema(description = "가맹점 이름", example = "GS25 역삼점") String storeName,
        @Schema(description = "단말 상태", example = "ACTIVE") String status) {

    public static TerminalAuthResponse from(StoreTerminal terminal) {
        return new TerminalAuthResponse(
                terminal.getId(),
                terminal.getStoreName(),
                terminal.getStatus().name());
    }
}
