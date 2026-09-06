package com.example.kidhackbackend.users.chat.service;

import com.example.kidhackbackend.users.chat.dto.ChatMessagePageResponse;
import com.example.kidhackbackend.users.chat.dto.ChatMessageRequest;
import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;
import com.example.kidhackbackend.users.chat.entity.ChatMessage;
import com.example.kidhackbackend.users.chat.entity.MessageType;
import com.example.kidhackbackend.users.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import com.example.kidhackbackend.users.chat.repository.ChatRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    /** 알림 본문에 실을 메시지 길이 상한. 길면 잘라서 붙인다. */
    private static final int PREVIEW_MAX = 40;

    private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final NotificationPort notificationPort;

    public ChatMessageResponse saveMessage(String roomId, ChatMessageRequest request) {
        // [VULN-CHAT-1] senderId를 클라이언트가 보낸 값 그대로 사용
        ChatMessage message = new ChatMessage(roomId, request.getSenderId(), request.getContent(),
                request.getType(), request.getMediaUrl(), null);
        chatMessageRepository.save(message);
        notifyCounterpart(roomId, message);
        return ChatMessageResponse.from(message);
    }

    /**
     * 방의 상대방에게 새 메시지 알림을 보낸다.
     *
     * <p>앱을 닫아 둔 사이에도 메시지가 온 걸 알 수 있어야 한다. WebSocket 은 앱이 떠 있을
     * 때만 닿는다.
     *
     * <p>알림 발행이 실패해도 <b>메시지 저장은 되돌리지 않는다.</b> 알림 때문에 채팅이
     * 실패하면 안 되기 때문이다. 실패는 로그만 남긴다.
     */
    private void notifyCounterpart(String roomId, ChatMessage message) {
        try {
            ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
            if (room == null) {
                return;
            }
            Long sender = message.getSenderId();
            Long recipient = Objects.equals(room.getParentId(), sender)
                    ? room.getChildId()
                    : room.getParentId();
            if (recipient == null || Objects.equals(recipient, sender)) {
                return;
            }
            notificationPort.publish(recipient, room.getChildId(),
                    NotificationType.CHAT_MESSAGE, preview(message));
        } catch (RuntimeException e) {
            log.warn("채팅 알림 발행 실패 roomId={}", roomId, e);
        }
    }

    private String preview(ChatMessage message) {
        // 미디어 메시지는 본문이 비어 있을 수 있으므로 종류별 자리표시로 대체한다.
        if (message.getType() == MessageType.IMAGE) {
            return "(사진)";
        }
        if (message.getType() == MessageType.AUDIO) {
            return "(음성)";
        }
        String content = message.getContent();
        if (content == null) {
            return "";
        }
        String oneLine = content.lines().findFirst().orElse("").trim();
        return (oneLine.length() <= PREVIEW_MAX)
                ? oneLine
                : oneLine.substring(0, PREVIEW_MAX) + "…";
    }

    /**
     * 커서 페이징. before 가 있으면 그 시각 이전 것만, 없으면 최신부터. 항상 최신 → 과거 순.
     *
     * <p>[VULN-CHAT-1] roomId만 알면 누구나 조회 가능 — 요청자가 이 방의 부모/자녀인지 검증하지 않는다.
     */
    public ChatMessagePageResponse getMessages(String roomId, LocalDateTime before, int size) {
        int limit = Math.max(1, Math.min(size, 100));
        PageRequest page = PageRequest.of(0, limit + 1); // hasMore 판정용으로 1건 더 읽는다

        List<ChatMessage> rows = (before == null)
                ? chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, page)
                : chatMessageRepository.findByRoomIdAndSentAtLessThanOrderBySentAtDesc(roomId, before, page);

        boolean hasMore = rows.size() > limit;
        List<ChatMessage> pageRows = hasMore ? rows.subList(0, limit) : rows;

        List<ChatMessageResponse> messages = new ArrayList<>(pageRows.size());
        for (ChatMessage m : pageRows) {
            messages.add(ChatMessageResponse.from(m));
        }
        LocalDateTime oldestSentAt = pageRows.isEmpty()
                ? null
                : pageRows.get(pageRows.size() - 1).getSentAt();

        return new ChatMessagePageResponse(messages, hasMore, oldestSentAt);
    }

    /** 방 목록에서 쓰는 안 읽은 개수. lastReadAt 이 없으면(한 번도 안 읽음) 전부 대상. */
    public long unreadCount(String roomId, LocalDateTime lastReadAt, Long memberId) {
        LocalDateTime after = (lastReadAt != null) ? lastReadAt : EPOCH;
        return chatMessageRepository.countByRoomIdAndSentAtGreaterThanAndSenderIdNot(roomId, after, memberId);
    }
}
