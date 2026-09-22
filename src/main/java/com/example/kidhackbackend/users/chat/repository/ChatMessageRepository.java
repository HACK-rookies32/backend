package com.example.kidhackbackend.users.chat.repository;

import com.example.kidhackbackend.users.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /** 커서 페이징 1페이지 — 최신순. Pageable 은 limit + sort 용도로만 넘긴다. */
    List<ChatMessage> findByRoomIdOrderBySentAtDesc(String roomId, Pageable pageable);

    /** 커서 페이징 다음 페이지 — before 시각 이전 것만, 최신순. */
    List<ChatMessage> findByRoomIdAndSentAtLessThanOrderBySentAtDesc(
            String roomId, LocalDateTime before, Pageable pageable);

    /** 방 목록의 "마지막 메시지" 표시용. */
    Optional<ChatMessage> findFirstByRoomIdOrderBySentAtDesc(String roomId);

    /** 안 읽은 개수 — 내 마지막 읽음 시각 이후에 온, 내가 보내지 않은 메시지. */
    long countByRoomIdAndSentAtGreaterThanAndSenderIdNot(
            String roomId, LocalDateTime after, Long senderId);

    /** 회원 탈퇴(물리삭제) 시 방의 메시지 정리. 방을 지우기 전에 방별로 부른다. */
    void deleteByRoomId(String roomId);
}
