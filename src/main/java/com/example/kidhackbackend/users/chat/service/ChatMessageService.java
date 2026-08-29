package com.example.kidhackbackend.users.chat.service;

import com.example.kidhackbackend.users.chat.dto.ChatMessagePageResponse;
import com.example.kidhackbackend.users.chat.dto.ChatMessageRequest;
import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;
import com.example.kidhackbackend.users.chat.entity.ChatMessage;
import com.example.kidhackbackend.users.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageResponse saveMessage(String roomId, ChatMessageRequest request) {
        // [VULN-CHAT-1] senderId를 클라이언트가 보낸 값 그대로 사용
        ChatMessage message = new ChatMessage(roomId, request.getSenderId(), request.getContent());
        chatMessageRepository.save(message);
        return ChatMessageResponse.from(message);
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
