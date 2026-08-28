package com.example.kidhackbackend.users.qrpay.dto;

import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 매장 단말 응답. 발급 시에는 terminalToken 을 포함해 돌려준다(단말에 주입할 값). */
@Schema(description = "매장 단말")
public record TerminalResponse(
        @Schema(description = "단말 ID", example = "1") Long id,
        @Schema(description = "가맹점 이름", example = "GS25 역삼점") String storeName,
        @Schema(description = "X-Store-Terminal-Token 값(결제 승인 호출에 사용 — 노출 주의)") String terminalToken,
        @Schema(description = "단말 상태", example = "ACTIVE") String status,
        @Schema(description = "생성 시각") LocalDateTime createdAt) {

    public static TerminalResponse from(StoreTerminal terminal) {
        return new TerminalResponse(
                terminal.getId(),
                terminal.getStoreName(),
                terminal.getTerminalToken(),
                terminal.getStatus().name(),
                terminal.getCreatedAt());
    }
}
