package com.example.kidhackbackend.users.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "채팅 메시지 전송 요청 (STOMP /pub/chat/{roomId} 본문)")
@Getter
public class ChatMessageRequest {

    @Schema(description = "채팅방 ID")
    private String roomId;

    @Schema(description = "보낸 사람 회원 ID. ⚠️ 서버가 이 값을 그대로 신뢰(인증 사용자와 대조 안 함) — 의도된 취약점", example = "1")
    private Long senderId;

    @Schema(description = "메시지 내용")
    private String content;
}
