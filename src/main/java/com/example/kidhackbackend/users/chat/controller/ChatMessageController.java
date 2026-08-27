package com.example.kidhackbackend.users.chat.controller;

import com.example.kidhackbackend.users.chat.broadcast.ChatBroadcastPort;
import com.example.kidhackbackend.users.chat.dto.ChatMessageRequest;
import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;
import com.example.kidhackbackend.users.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    // SimpMessagingTemplate 을 직접 쓰지 않는다. 그렇게 하면 WAS 가 2 대일 때
    // 다른 인스턴스에 붙은 상대에게 메시지가 가지 않는다 (ChatBroadcastPort 주석 참고).
    private final ChatBroadcastPort chatBroadcastPort;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, ChatMessageRequest request) {
        ChatMessageResponse response = chatMessageService.saveMessage(roomId, request);
        chatBroadcastPort.broadcast(roomId, response);
    }
}