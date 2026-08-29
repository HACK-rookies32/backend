package com.example.kidhackbackend.users.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "채팅방 생성/조회 요청 (부모는 인증 토큰에서 식별)")
@Getter
public class ChatRoomRequest {

    @Schema(description = "자녀 회원 ID. 로그인한 부모의 자녀여야 한다.", example = "7",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long childId;
}
