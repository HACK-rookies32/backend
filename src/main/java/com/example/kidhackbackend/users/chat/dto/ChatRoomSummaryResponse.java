package com.example.kidhackbackend.users.chat.dto;

import com.example.kidhackbackend.users.chat.entity.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "채팅방 목록 항목 (상대방 정보 · 마지막 메시지 · 안 읽은 수 포함)")
@Getter
@AllArgsConstructor
public class ChatRoomSummaryResponse {

    @Schema(description = "채팅방 ID")
    private String roomId;

    @Schema(description = "부모 회원 ID", example = "1")
    private Long parentId;

    @Schema(description = "자녀 회원 ID", example = "7")
    private Long childId;

    @Schema(description = "상대방 회원 ID (요청자가 부모면 자녀, 자녀면 부모)", example = "7")
    private Long counterpartId;

    @Schema(description = "상대방 닉네임", example = "민준")
    private String counterpartNickname;

    @Schema(description = "상대방 실명", example = "김민준")
    private String counterpartName;

    @Schema(description = "상대방 프로필 이미지 URL (없으면 null)")
    private String counterpartProfileImageUrl;

    @Schema(description = "마지막 메시지 미리보기 (TEXT는 본문, IMAGE는 \"사진\", AUDIO는 \"음성 메시지\"; 아직 없으면 null)")
    private String lastMessage;

    @Schema(description = "마지막 메시지 종류 (TEXT/IMAGE/AUDIO; 아직 없으면 null)")
    private MessageType lastMessageType;

    @Schema(description = "마지막 메시지 시각 (아직 없으면 null)")
    private LocalDateTime lastMessageAt;

    @Schema(description = "내가 안 읽은 메시지 수", example = "3")
    private long unreadCount;

    @Schema(description = "방 생성 시각")
    private LocalDateTime createdAt;
}
