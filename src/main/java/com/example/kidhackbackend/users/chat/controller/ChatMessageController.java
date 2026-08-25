package com.example.kidhackbackend.users.chat.controller;

import com.example.kidhackbackend.users.chat.dto.ChatMessageRequest;
import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;
import com.example.kidhackbackend.users.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, ChatMessageRequest request) {
        ChatMessageResponse response = chatMessageService.saveMessage(roomId, request);
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, response);
    }
}