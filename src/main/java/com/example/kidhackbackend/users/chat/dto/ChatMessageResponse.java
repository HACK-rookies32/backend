package com.example.kidhackbackend.users.chat.dto;

import com.example.kidhackbackend.users.chat.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessageResponse {
    private String id;
    private String roomId;
    private Long senderId;
    private String content;
    private LocalDateTime sentAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRoomId(),
                message.getSenderId(), message.getContent(), message.getSentAt());
    }
}