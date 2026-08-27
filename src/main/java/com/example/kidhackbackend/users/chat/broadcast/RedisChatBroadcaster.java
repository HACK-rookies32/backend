package com.example.kidhackbackend.users.chat.broadcast;

import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis 채널에 발행만 한다. 자기 인스턴스로 보내는 일은 하지 않는다 —
 * 발행자도 같은 채널을 구독하고 있어서 RedisChatSubscriber 가 대신 처리한다.
 * 여기서 로컬 전송을 같이 하면 보낸 사람에게 메시지가 두 번 간다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.chat.broker", havingValue = "redis")
public class RedisChatBroadcaster implements ChatBroadcastPort {

    /** 방마다 채널을 파지 않는다. 구독을 동적으로 등록/해제할 일이 없어진다. */
    public static final String CHANNEL = "chat";

    private final StringRedisTemplate redisTemplate;

    // Boot 가 만든 빈이어야 한다. new ObjectMapper() 로 직접 만들면 설정이 달라져
    // Redis 를 거친 JSON 이 기존 STOMP 출력과 어긋날 수 있다.
    // Boot 4 는 Jackson 3(tools.jackson) 만 빈으로 등록한다 — com.fasterxml 쪽은 빈이 없다.
    private final ObjectMapper objectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcast(String roomId, ChatMessageResponse message) {
        try {
            redisTemplate.convertAndSend(CHANNEL,
                    objectMapper.writeValueAsString(new ChatBroadcast(roomId, message)));
        } catch (JacksonException | DataAccessException e) {
            // 발행이 실패했으면 아무도 받지 못한 상태다. 그러니 여기서 로컬로 한 번
            // 보내도 중복이 생기지 않는다. Redis 가 죽어도 같은 인스턴스에 붙은
            // 사용자끼리는 계속 되게 해서 전면 장애로 번지는 것을 막는다.
            log.warn("Redis 브로드캐스트 실패, 로컬 전달로 폴백: roomId={}", roomId, e);
            messagingTemplate.convertAndSend(ChatDestination.of(roomId), message);
        }
    }
}
