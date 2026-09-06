package com.example.kidhackbackend.users.chat.dto;

import com.example.kidhackbackend.users.chat.entity.ChatMessage;
import com.example.kidhackbackend.users.chat.entity.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "채팅 메시지")
@Getter
@AllArgsConstructor
public class ChatMessageResponse {

    @Schema(description = "메시지 ID")
    private String id;

    @Schema(description = "채팅방 ID")
    private String roomId;

    @Schema(description = "보낸 사람 회원 ID", example = "1")
    private Long senderId;

    @Schema(description = "메시지 내용")
    private String content;

    @Schema(description = "메시지 종류")
    private MessageType type;

    @Schema(description = "첨부 미디어 공개 경로(/api/files/chat/{id}). TEXT 면 null.")
    private String mediaUrl;

    @Schema(description = "첨부 미디어 Content-Type. TEXT 면 null.")
    private String mediaContentType;

    @Schema(description = "전송 시각")
    private LocalDateTime sentAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRoomId(),
                message.getSenderId(), message.getContent(),
                message.getType(), message.getMediaUrl(), message.getMediaContentType(),
                message.getSentAt());
    }
}
