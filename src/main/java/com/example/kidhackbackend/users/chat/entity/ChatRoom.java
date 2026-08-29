package com.example.kidhackbackend.users.chat.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@Document(collection = "chat_rooms")
public class ChatRoom {

    @Id
    private String id;
    private Long parentId;
    private Long childId;
    private LocalDateTime createdAt;

    /**
     * 참여자별 마지막 읽음 시각. key 는 memberId(문자열 — Mongo Map key 제약), value 는 그 시점.
     * 저장된 문서에 이 필드가 없으면(구버전) null 일 수 있으므로 {@link #lastReadAtOf}/{@link #markRead}
     * 로만 접근한다.
     */
    private Map<String, LocalDateTime> lastReadAt = new HashMap<>();

    public ChatRoom(Long parentId, Long childId) {
        this.parentId = parentId;
        this.childId = childId;
        this.createdAt = LocalDateTime.now();
        this.lastReadAt = new HashMap<>();
    }

    public boolean hasParticipant(Long memberId) {
        return memberId != null && (memberId.equals(parentId) || memberId.equals(childId));
    }

    /** memberId 가 부모면 상대는 자녀, 자녀면 상대는 부모. 참여자가 아니면 null. */
    public Long counterpartOf(Long memberId) {
        if (memberId == null) return null;
        if (memberId.equals(parentId)) return childId;
        if (memberId.equals(childId)) return parentId;
        return null;
    }

    public LocalDateTime lastReadAtOf(Long memberId) {
        if (lastReadAt == null || memberId == null) return null;
        return lastReadAt.get(memberId.toString());
    }

    public void markRead(Long memberId, LocalDateTime at) {
        if (lastReadAt == null) {
            lastReadAt = new HashMap<>();
        }
        lastReadAt.put(memberId.toString(), at);
    }
}
