package com.example.kidhackbackend.chat;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.chat.dto.ChatMessageRequest;
import com.example.kidhackbackend.users.chat.entity.ChatMessage;
import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import com.example.kidhackbackend.users.chat.repository.ChatMessageRepository;
import com.example.kidhackbackend.users.chat.repository.ChatRoomRepository;
import com.example.kidhackbackend.users.chat.service.ChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 채팅 메시지를 저장하면 <b>상대방에게</b> 알림이 가는지.
 *
 * <p>WebSocket 은 앱이 떠 있을 때만 닿으므로, 앱을 닫아 둔 사이에 온 메시지는 알림으로만
 * 알 수 있다. 채팅은 MongoDB 기반이라 컨텍스트를 띄우지 않고 저장소만 대역으로 바꿔 검증한다.
 */
class ChatMessageNotificationTest {

    private static final Long PARENT = 1L;
    private static final Long CHILD = 2L;
    private static final String ROOM = "room-1";

    /** 발행된 알림을 기록하는 대역. */
    private static class RecordingPort implements NotificationPort {
        record Published(Long recipientId, Long childId, NotificationType type, String message) { }

        final List<Published> published = new ArrayList<>();

        @Override
        public void publish(Long recipientId, NotificationType type, String message) {
            publish(recipientId, null, type, message);
        }

        @Override
        public void publish(Long recipientId, Long childId, NotificationType type, String message) {
            published.add(new Published(recipientId, childId, type, message));
        }
    }

    private RecordingPort port;
    private ChatMessageRepository messageRepository;
    private ChatRoomRepository roomRepository;
    private ChatMessageService service;

    @BeforeEach
    void setUp() {
        port = new RecordingPort();
        messageRepository = mock(ChatMessageRepository.class);
        roomRepository = mock(ChatRoomRepository.class);
        when(messageRepository.save(any(ChatMessage.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChatRoom room = new ChatRoom(PARENT, CHILD);
        when(roomRepository.findById(ROOM)).thenReturn(Optional.of(room));

        service = new ChatMessageService(messageRepository, roomRepository, port);
    }

    private ChatMessageRequest request(Long senderId, String content) {
        ChatMessageRequest r = new ChatMessageRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(r, "senderId", senderId);
        org.springframework.test.util.ReflectionTestUtils.setField(r, "content", content);
        return r;
    }

    @Test
    void 부모가_보내면_자녀에게_알림이_간다() {
        service.saveMessage(ROOM, request(PARENT, "밥 먹었니?"));

        assertThat(port.published).hasSize(1);
        RecordingPort.Published p = port.published.get(0);
        assertThat(p.recipientId()).isEqualTo(CHILD);
        assertThat(p.type()).isEqualTo(NotificationType.CHAT_MESSAGE);
        // 어느 자녀의 대화인지 구분할 수 있어야 한다.
        assertThat(p.childId()).isEqualTo(CHILD);
        assertThat(p.message()).isEqualTo("밥 먹었니?");
    }

    @Test
    void 자녀가_보내면_부모에게_알림이_간다() {
        service.saveMessage(ROOM, request(CHILD, "학원 끝났어요"));

        assertThat(port.published).hasSize(1);
        assertThat(port.published.get(0).recipientId()).isEqualTo(PARENT);
        assertThat(port.published.get(0).childId()).isEqualTo(CHILD);
    }

    @Test
    void 긴_메시지는_잘라서_알린다() {
        String long50 = "가".repeat(50);

        service.saveMessage(ROOM, request(PARENT, long50));

        String message = port.published.get(0).message();
        assertThat(message).hasSize(41).endsWith("…");
    }

    @Test
    void 여러_줄_메시지는_첫_줄만_알린다() {
        service.saveMessage(ROOM, request(PARENT, "첫 줄\n둘째 줄"));

        assertThat(port.published.get(0).message()).isEqualTo("첫 줄");
    }

    /**
     * 알림 발행이 실패해도 메시지 저장은 유지돼야 한다.
     * 알림 때문에 채팅이 실패하면 안 된다.
     */
    @Test
    void 방을_찾지_못해도_메시지_저장은_성공한다() {
        when(roomRepository.findById(ROOM)).thenReturn(Optional.empty());

        assertThat(service.saveMessage(ROOM, request(PARENT, "안녕")).getContent()).isEqualTo("안녕");
        assertThat(port.published).isEmpty();
    }

    @Test
    void 알림_발행이_터져도_메시지_저장은_성공한다() {
        NotificationPort failing = Mockito.mock(NotificationPort.class);
        Mockito.doThrow(new IllegalStateException("boom"))
                .when(failing).publish(any(), any(), any(), any());
        ChatMessageService s = new ChatMessageService(messageRepository, roomRepository, failing);

        assertThat(s.saveMessage(ROOM, request(PARENT, "안녕")).getContent()).isEqualTo("안녕");
    }
}
