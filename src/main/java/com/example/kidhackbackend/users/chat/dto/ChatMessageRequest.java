package com.example.kidhackbackend.users.chat.dto;

import lombok.Getter;

@Getter
public class ChatMessageRequest {
    private String roomId;
    private Long senderId; // [VULN-CHAT-1] 서버가 이 값을 그대로 신뢰함, 인증된 사용자와 대조 안 함
    private String content;
}