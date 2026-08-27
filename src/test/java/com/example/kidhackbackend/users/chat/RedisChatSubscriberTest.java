package com.example.kidhackbackend.users.chat;

import com.example.kidhackbackend.users.chat.broadcast.ChatBroadcast;
import com.example.kidhackbackend.users.chat.broadcast.RedisChatSubscriber;
import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 에서 받은 봉투를 자기 인스턴스의 STOMP 구독자에게 넘기는 부분만 떼어 본다.
 * 실제 Redis 없이 onMessage 를 직접 호출하고, 보내진 메시지를 채널에서 가로챈다.
 */
@SpringBootTest
class RedisChatSubscriberTest {

    @Autowired
    ObjectMapper objectMapper;

    private final ChatMessageResponse response = new ChatMessageResponse(
            "6a8d554adac994ff3737d5e8", "6a8d4054ce8ce918a188ad9a", 1L, "안녕",
            LocalDateTime.of(2026, 8, 25, 17, 41, 46, 483_000_000));

    @Test
    void 봉투의_roomId_로_destination_을_만들고_페이로드는_그대로_넘긴다() {
        List<Message<?>> sent = new ArrayList<>();
        SimpMessagingTemplate template = new SimpMessagingTemplate((message, timeout) -> sent.add(message));

        RedisChatSubscriber subscriber = new RedisChatSubscriber(template, objectMapper);

        String published = objectMapper.writeValueAsString(new ChatBroadcast(response.getRoomId(), response));
        subscriber.onMessage(new DefaultMessage("chat".getBytes(StandardCharsets.UTF_8),
                published.getBytes(StandardCharsets.UTF_8)), null);

        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER))
                .isEqualTo("/topic/chat/" + response.getRoomId());

        // STOMP 로 넘어가는 페이로드가 DTO 를 직접 직렬화한 것과 글자까지 같아야 한다.
        assertThat(objectMapper.writeValueAsString(sent.get(0).getPayload()))
                .isEqualTo(objectMapper.writeValueAsString(response));
    }

    @Test
    void 망가진_메시지가_와도_리스너가_죽지_않는다() {
        List<Message<?>> sent = new ArrayList<>();
        SimpMessagingTemplate template = new SimpMessagingTemplate((message, timeout) -> sent.add(message));

        RedisChatSubscriber subscriber = new RedisChatSubscriber(template, objectMapper);

        // 예외가 밖으로 나가면 리스너 스레드가 끊겨 이후 메시지를 전부 놓친다.
        subscriber.onMessage(new DefaultMessage("chat".getBytes(StandardCharsets.UTF_8),
                "{ not json".getBytes(StandardCharsets.UTF_8)), null);

        assertThat(sent).isEmpty();
    }
}
