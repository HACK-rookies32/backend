package com.example.kidhackbackend.users.chat.broadcast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis 채널에서 받은 메시지를 자기 인스턴스에 붙어 있는 구독자에게 전달한다.
 * 발행한 인스턴스에도 똑같이 도착하므로, 발행부는 로컬 전송을 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.chat.broker", havingValue = "redis")
public class RedisChatSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode envelope = objectMapper.readTree(message.getBody());

            // 페이로드를 ChatMessageResponse 로 되돌리지 않고 JsonNode 그대로 넘긴다.
            // 발행 때 만들어진 JSON 이 글자 그대로 클라이언트까지 가므로 전송 포맷이
            // 바뀔 여지가 없다. 릴레이는 내용을 해석할 이유도 없다.
            messagingTemplate.convertAndSend(
                    ChatDestination.of(envelope.path("roomId").asString()),
                    envelope.path("message"));
        } catch (RuntimeException e) {
            // 한 건이 깨져도 리스너 스레드는 살아 있어야 다음 메시지를 받는다.
            log.warn("채팅 브로드캐스트 수신 처리 실패", e);
        }
    }
}
