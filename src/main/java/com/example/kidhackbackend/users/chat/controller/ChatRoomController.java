package com.example.kidhackbackend.users.chat.controller;
import com.example.kidhackbackend.users.chat.dto.*;
import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import com.example.kidhackbackend.users.chat.service.ChatMessageService;
import com.example.kidhackbackend.users.chat.service.ChatRoomService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createOrGetRoom(@RequestBody ChatRoomRequest request) {
        ChatRoom room = chatRoomService.getOrCreateRoom(request.getParentId(), request.getChildId());
        return ResponseEntity.ok(ApiResponse.success(ChatRoomResponse.from(room)));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getMessages(
            @PathVariable String roomId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ChatMessageResponse> messages = chatMessageService.getMessages(roomId, pageable);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
}