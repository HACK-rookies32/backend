package com.example.kidhackbackend.users.chat.dto;

import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "채팅방")
@Getter
@AllArgsConstructor
public class ChatRoomResponse {

    @Schema(description = "채팅방 ID")
    private String id;

    @Schema(description = "부모 회원 ID", example = "1")
    private Long parentId;

    @Schema(description = "자녀 회원 ID", example = "7")
    private Long childId;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(room.getId(), room.getParentId(), room.getChildId(), room.getCreatedAt());
    }
}
