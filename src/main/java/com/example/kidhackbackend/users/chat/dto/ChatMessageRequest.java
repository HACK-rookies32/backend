package com.example.kidhackbackend.users.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "채팅 메시지 전송 요청 (STOMP 발행 /pub/chat/{roomId} 의 본문). roomId 는 목적지 경로에서 받으므로 본문에 넣지 않는다.")
@Getter
public class ChatMessageRequest {

    @Schema(description = "보낸 사람 회원 ID. ⚠️ [VULN-CHAT-1] 서버가 이 값을 그대로 신뢰(인증 사용자와 대조 안 함) — 의도된 취약점",
            example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long senderId;

    @Schema(description = "메시지 내용 (텍스트 전용)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
