package com.example.kidhackbackend.users.chat.migration;

import com.example.kidhackbackend.users.chat.entity.ChatMessage;
import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 같은 (parentId, childId) 쌍에 여러 개 생겨 버린 채팅방을 하나로 합친다.
 *
 * <p>ChatRoom 에 유니크 제약이 없어 방이 중복 생성됐고, 그 결과 메시지가 방마다 흩어져
 * "앱이 여는 방"과 "메시지가 있는 방"이 어긋나 채팅 내역이 안 보이는 문제가 있었다.
 *
 * <p>부팅 시 1회 실행하되 <b>멱등</b>하다 — 중복이 없으면 아무것도 바꾸지 않는다.
 * 정리 순서:
 * <ol>
 *   <li>(parentId, childId) 로 그룹핑, 원소가 2개 이상인 그룹만 대상.</li>
 *   <li>가장 오래된 방(createdAt 최소, 동률이면 id 사전순)을 <b>정본</b>으로 남긴다.</li>
 *   <li>나머지 방에 달린 chat_messages 의 roomId 를 정본 방 id 로 갱신한다.</li>
 *   <li>나머지 방의 lastReadAt(참여자별 읽음 시각)을 정본 방에 최신값으로 병합한다.</li>
 *   <li>나머지 방을 삭제한다.</li>
 * </ol>
 * 마지막으로 (parentId, childId) <b>유니크 인덱스</b>를 만들어 재발을 막는다
 * (중복을 먼저 없앤 뒤라 인덱스 생성이 실패하지 않는다).
 */
@Component
@Order(0)
public class ChatRoomDedupMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomDedupMigration.class);

    private final MongoTemplate mongoTemplate;

    public ChatRoomDedupMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int mergedRooms = mergeDuplicateRooms();
            ensureUniqueIndex();
            if (mergedRooms > 0) {
                log.info("[chat-dedup] 중복 채팅방 {}개를 정본 방으로 병합했다.", mergedRooms);
            }
        } catch (DataAccessException e) {
            // 마이그레이션 실패가 앱 기동을 막지는 않게 한다. 원인은 로그로 남긴다.
            log.warn("[chat-dedup] 중복 방 정리/인덱스 생성 실패 — 건너뛴다.", e);
        }
    }

    /** @return 삭제된(병합된) 중복 방 개수. */
    private int mergeDuplicateRooms() {
        List<ChatRoom> rooms = mongoTemplate.findAll(ChatRoom.class);

        // (parentId|childId) → 그 쌍의 방들. 삽입 순서 유지.
        Map<String, List<ChatRoom>> byPair = new LinkedHashMap<>();
        for (ChatRoom room : rooms) {
            if (room.getParentId() == null || room.getChildId() == null) {
                continue; // 짝이 불완전한 방은 건드리지 않는다.
            }
            String key = room.getParentId() + "|" + room.getChildId();
            byPair.computeIfAbsent(key, k -> new ArrayList<>()).add(room);
        }

        int merged = 0;
        for (List<ChatRoom> group : byPair.values()) {
            if (group.size() < 2) {
                continue;
            }
            // 가장 오래된 방을 정본으로. createdAt 이 null 이면 가장 뒤로.
            group.sort((a, b) -> {
                LocalDateTime ca = a.getCreatedAt();
                LocalDateTime cb = b.getCreatedAt();
                if (ca == null && cb == null) return a.getId().compareTo(b.getId());
                if (ca == null) return 1;
                if (cb == null) return -1;
                int c = ca.compareTo(cb);
                return (c != 0) ? c : a.getId().compareTo(b.getId());
            });
            ChatRoom canonical = group.get(0);

            for (int i = 1; i < group.size(); i++) {
                ChatRoom dup = group.get(i);
                // 1) 이 방의 메시지를 정본 방으로 이관.
                mongoTemplate.updateMulti(
                        Query.query(Criteria.where("roomId").is(dup.getId())),
                        Update.update("roomId", canonical.getId()),
                        ChatMessage.class);
                // 2) 읽음 시각 병합(더 최신 값만 남긴다).
                mergeLastReadInto(canonical, dup);
                // 3) 중복 방 삭제.
                mongoTemplate.remove(dup);
                merged++;
            }
            // 병합된 읽음 시각을 정본 방에 저장.
            mongoTemplate.save(canonical);
        }
        return merged;
    }

    private void mergeLastReadInto(ChatRoom canonical, ChatRoom dup) {
        Map<String, LocalDateTime> from = dup.getLastReadAt();
        if (from == null || from.isEmpty()) {
            return;
        }
        for (Map.Entry<String, LocalDateTime> e : from.entrySet()) {
            Long memberId = parseMemberId(e.getKey());
            if (memberId == null) {
                continue; // key 가 회원 id 형식이 아니면 건너뛴다.
            }
            // 엔티티의 null-safe 접근만 쓴다(구버전 문서는 lastReadAt 이 null 일 수 있다).
            LocalDateTime existing = canonical.lastReadAtOf(memberId);
            if (existing == null || (e.getValue() != null && e.getValue().isAfter(existing))) {
                canonical.markRead(memberId, e.getValue());
            }
        }
    }

    private Long parseMemberId(String key) {
        try {
            return Long.valueOf(key);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void ensureUniqueIndex() {
        // 중복을 먼저 없앴으므로 유니크 인덱스 생성이 안전하다.
        // 이미 존재하면 같은 정의로 재요청해도 무해하다(멱등).
        mongoTemplate.indexOps(ChatRoom.class).createIndex(
                new Index()
                        .on("parentId", Sort.Direction.ASC)
                        .on("childId", Sort.Direction.ASC)
                        .unique()
                        .named("uk_chat_room_parent_child"));
    }
}
