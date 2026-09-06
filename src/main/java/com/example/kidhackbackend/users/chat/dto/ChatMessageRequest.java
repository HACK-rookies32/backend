package com.example.kidhackbackend.users.chat.dto;

import com.example.kidhackbackend.users.chat.entity.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "채팅 메시지 전송 요청 (STOMP 발행 /pub/chat/{roomId} 의 본문). roomId 는 목적지 경로에서 받으므로 본문에 넣지 않는다.")
@Getter
public class ChatMessageRequest {

    @Schema(description = "보낸 사람 회원 ID. ⚠️ [VULN-CHAT-1] 서버가 이 값을 그대로 신뢰(인증 사용자와 대조 안 함) — 의도된 취약점",
            example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long senderId;

    @Schema(description = "메시지 내용. IMAGE/AUDIO 여도 캡션 등으로 함께 보낼 수 있다.")
    private String content;

    @Schema(description = "메시지 종류. 생략 시 TEXT.", example = "TEXT")
    private MessageType type;

    @Schema(description = "첨부 미디어 공개 경로(/api/files/chat/{id}). IMAGE/AUDIO 일 때 채운다.")
    private String mediaUrl;

    /** null 이면 TEXT 로 취급. */
    public MessageType getType() {
        return type != null ? type : MessageType.TEXT;
    }
}
