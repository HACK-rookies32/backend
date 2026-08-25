package com.example.kidhackbackend.users.chat.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Document(collection = "chat_rooms")
public class ChatRoom {

    @Id
    private String id;
    private Long parentId;
    private Long childId;
    private LocalDateTime createdAt;

    public ChatRoom(Long parentId, Long childId) {
        this.parentId = parentId;
        this.childId = childId;
        this.createdAt = LocalDateTime.now();
    }
}