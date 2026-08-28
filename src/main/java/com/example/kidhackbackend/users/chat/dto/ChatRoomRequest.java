package com.example.kidhackbackend.users.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "채팅방 생성/조회 요청")
@Getter
public class ChatRoomRequest {

    @Schema(description = "부모 회원 ID", example = "1")
    private Long parentId;

    @Schema(description = "자녀 회원 ID", example = "7")
    private Long childId;
}
