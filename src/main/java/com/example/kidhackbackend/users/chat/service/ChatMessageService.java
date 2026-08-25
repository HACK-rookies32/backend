package com.example.kidhackbackend.users.chat.service;

import com.example.kidhackbackend.users.chat.dto.ChatMessageRequest;
import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;
import com.example.kidhackbackend.users.chat.entity.ChatMessage;
import com.example.kidhackbackend.users.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageResponse saveMessage(String roomId, ChatMessageRequest request) {
        // [VULN-CHAT-1] senderId를 클라이언트가 보낸 값 그대로 사용
        ChatMessage message = new ChatMessage(roomId, request.getSenderId(), request.getContent());
        chatMessageRepository.save(message);
        return ChatMessageResponse.from(message);
    }

    public Page<ChatMessageResponse> getMessages(String roomId, Pageable pageable) {
        // [VULN-CHAT-1] roomId만 알면 누구나 조회 가능, 요청자가 이 방의 부모/자녀인지 검증 안 함
        return chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable)
                .map(ChatMessageResponse::from);
    }
}