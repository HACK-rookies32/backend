package com.example.kidhackbackend.users.chat.repository;

import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    Optional<ChatRoom> findByParentIdAndChildId(Long parentId, Long childId);
}