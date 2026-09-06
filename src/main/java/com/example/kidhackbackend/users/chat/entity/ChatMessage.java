package com.example.kidhackbackend.users.chat.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;
    private String roomId;
    private Long senderId;
    private String content; // [VULN-CHAT-2] XSS 이스케이프 없이 그대로 저장/전달
    private MessageType type;
    private String mediaUrl;         // null for TEXT
    private String mediaContentType; // null for TEXT
    private LocalDateTime sentAt;

    public ChatMessage(String roomId, Long senderId, String content) {
        this(roomId, senderId, content, MessageType.TEXT, null, null);
    }

    public ChatMessage(String roomId, Long senderId, String content,
                       MessageType type, String mediaUrl, String mediaContentType) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.type = (type != null) ? type : MessageType.TEXT;
        this.mediaUrl = mediaUrl;
        this.mediaContentType = mediaContentType;
        this.sentAt = LocalDateTime.now();
    }
}