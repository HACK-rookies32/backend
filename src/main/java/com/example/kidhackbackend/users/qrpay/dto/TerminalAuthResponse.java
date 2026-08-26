package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;

/**
 * 매장 단말 인증 확인 응답 (X-Store-Terminal-Token 검증 결과). 토큰 값은 담지 않는다.
 */
public record TerminalAuthResponse(
        Long terminalId,
        String storeName,
        String status) {

    public static TerminalAuthResponse from(StoreTerminal terminal) {
        return new TerminalAuthResponse(
                terminal.getId(),
                terminal.getStoreName(),
                terminal.getStatus().name());
    }
}
