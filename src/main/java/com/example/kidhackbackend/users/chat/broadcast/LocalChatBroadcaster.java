package com.example.kidhackbackend.users.chat.broadcast;

import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 이 JVM 에 붙어 있는 구독자에게만 전달한다. Redis 없이 도는 로컬/CI 기본값이다.
 * WAS 가 2 대 이상인 환경에서 이걸 쓰면 절반은 메시지를 못 받는다 — 운영은 redis 로 둘 것.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.chat.broker", havingValue = "local", matchIfMissing = true)
public class LocalChatBroadcaster implements ChatBroadcastPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(String roomId, ChatMessageResponse message) {
        messagingTemplate.convertAndSend(ChatDestination.of(roomId), message);
    }
}
