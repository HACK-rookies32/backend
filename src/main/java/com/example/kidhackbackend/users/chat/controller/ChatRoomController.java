package com.example.kidhackbackend.users.chat.controller;
import com.example.kidhackbackend.users.chat.dto.*;
import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import com.example.kidhackbackend.users.chat.service.ChatMessageService;
import com.example.kidhackbackend.users.chat.service.ChatRoomService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "22. 채팅", description = "채팅방 생성/조회 · 메시지 페이징 조회 (실시간 송수신은 STOMP WebSocket: /ws, 구독 /sub/chat/{roomId}, 발행 /pub/chat/{roomId})")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @Operation(summary = "채팅방 생성 또는 조회",
            description = "부모-자녀 조합의 채팅방이 있으면 반환하고, 없으면 생성한다.")
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createOrGetRoom(@RequestBody ChatRoomRequest request) {
        ChatRoom room = chatRoomService.getOrCreateRoom(request.getParentId(), request.getChildId());
        return ResponseEntity.ok(ApiResponse.success(ChatRoomResponse.from(room)));
    }

    @Operation(summary = "채팅방 메시지 목록 조회",
            description = "채팅방의 과거 메시지를 페이징 조회한다(기본 20건).")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getMessages(
            @Parameter(description = "채팅방 ID") @PathVariable String roomId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ChatMessageResponse> messages = chatMessageService.getMessages(roomId, pageable);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}
