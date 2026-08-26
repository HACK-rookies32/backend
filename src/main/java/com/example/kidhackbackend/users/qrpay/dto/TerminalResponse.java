package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;
import java.time.LocalDateTime;

/**
 * 매장 단말 응답. 발급 시에는 {@code terminalToken}을 포함해 돌려준다(단말에 주입할 값).
 */
public record TerminalResponse(
        Long id,
        String storeName,
        String terminalToken,
        String status,
        LocalDateTime createdAt) {

    public static TerminalResponse from(StoreTerminal terminal) {
        return new TerminalResponse(
                terminal.getId(),
                terminal.getStoreName(),
                terminal.getTerminalToken(),
                terminal.getStatus().name(),
                terminal.getCreatedAt());
    }
}
