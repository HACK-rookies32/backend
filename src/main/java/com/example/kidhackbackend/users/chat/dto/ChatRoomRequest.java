package com.example.kidhackbackend.users.chat.dto;

import lombok.Getter;

@Getter
public class ChatRoomRequest {
    private Long parentId;
    private Long childId;
}