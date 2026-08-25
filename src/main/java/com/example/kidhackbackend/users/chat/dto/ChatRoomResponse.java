package com.example.kidhackbackend.users.chat.dto;

import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomResponse {
    private String id;
    private Long parentId;
    private Long childId;
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(room.getId(), room.getParentId(), room.getChildId(), room.getCreatedAt());
    }
}