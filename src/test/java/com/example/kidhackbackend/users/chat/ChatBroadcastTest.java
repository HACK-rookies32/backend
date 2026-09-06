package com.example.kidhackbackend.users.chat;

import com.example.kidhackbackend.users.chat.broadcast.ChatBroadcast;
import com.example.kidhackbackend.users.chat.broadcast.ChatBroadcastPort;
import com.example.kidhackbackend.users.chat.broadcast.RedisChatSubscriber;
import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 를 거치면 JSON 직렬화/역직렬화가 한 번 더 생긴다. 그 과정에서 전송 포맷이
 * 바뀌면 구독 중인 클라이언트가 깨지는데, 메시지가 오는 것만 눈으로 보면 놓친다.
 * 여기서 포맷과 릴레이 동작을 고정해 둔다.
 */
@SpringBootTest
class ChatBroadcastTest {

    @Autowired
    ApplicationContext ctx;

    /** Boot 가 STOMP 전송에 쓰는 것과 같은 빈이어야 의미가 있다. */
    @Autowired
    ObjectMapper objectMapper;

    private final ChatMessageResponse response = new ChatMessageResponse(
            "6a8d554adac994ff3737d5e8", "6a8d4054ce8ce918a188ad9a", 1L, "안녕",
            com.example.kidhackbackend.users.chat.entity.MessageType.TEXT, null, null,
            LocalDateTime.of(2026, 8, 25, 17, 41, 46, 483_000_000));

    @Test
    void sentAt_은_ISO_문자열로_직렬화된다() {
        String json = objectMapper.writeValueAsString(new ChatBroadcast(response.getRoomId(), response));

        // 배열([2026,8,25,...])로 나가면 명세와 달라져 클라이언트가 깨진다.
        assertThat(json).contains("\"sentAt\":\"2026-08-25T17:41:46.483\"");
        // 타입 정보가 섞여 나가면 페이로드가 오염된다.
        assertThat(json).doesNotContain("@class");
    }

    @Test
    void Redis_를_거쳐도_전송_페이로드가_그대로다() {
        String direct = objectMapper.writeValueAsString(response);

        // 발행 -> 수신 -> 릴레이 경로를 그대로 재현한다.
        String published = objectMapper.writeValueAsString(new ChatBroadcast(response.getRoomId(), response));
        JsonNode relayed = objectMapper.readTree(published).path("message");

        assertThat(objectMapper.writeValueAsString(relayed)).isEqualTo(direct);
    }

    @Test
    void 브로커_구현체는_정확히_하나만_등록된다() {
        // 둘 다 뜨면 주입이 실패하고, 하나도 안 뜨면 컨트롤러가 기동하지 못한다.
        assertThat(ctx.getBeanNamesForType(ChatBroadcastPort.class)).hasSize(1);
    }

    @Test
    void 봉투의_roomId_로_destination_을_만들고_페이로드는_그대로_넘긴다() {
        List<Message<?>> sent = new ArrayList<>();
        RedisChatSubscriber subscriber = subscriberCapturing(sent);

        String published = objectMapper.writeValueAsString(new ChatBroadcast(response.getRoomId(), response));
        subscriber.onMessage(redisMessage(published), null);

        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER))
                .isEqualTo("/sub/chat/" + response.getRoomId());

        // STOMP 로 넘어가는 페이로드가 DTO 를 직접 직렬화한 것과 글자까지 같아야 한다.
        assertThat(objectMapper.writeValueAsString(sent.get(0).getPayload()))
                .isEqualTo(objectMapper.writeValueAsString(response));
    }

    @Test
    void 망가진_메시지가_와도_리스너가_죽지_않는다() {
        List<Message<?>> sent = new ArrayList<>();
        RedisChatSubscriber subscriber = subscriberCapturing(sent);

        // 예외가 밖으로 나가면 리스너 스레드가 끊겨 이후 메시지를 전부 놓친다.
        subscriber.onMessage(redisMessage("{ not json"), null);

        assertThat(sent).isEmpty();
    }

    /** 실제 Redis 없이 onMessage 를 직접 부르고, 보내진 메시지를 채널에서 가로챈다. */
    private RedisChatSubscriber subscriberCapturing(List<Message<?>> sent) {
        return new RedisChatSubscriber(
                new SimpMessagingTemplate((message, timeout) -> sent.add(message)), objectMapper);
    }

    private DefaultMessage redisMessage(String body) {
        return new DefaultMessage("chat".getBytes(StandardCharsets.UTF_8),
                body.getBytes(StandardCharsets.UTF_8));
    }
}
