package com.example.kidhackbackend.users.chat.repository;

import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    Optional<ChatRoom> findByParentIdAndChildId(Long parentId, Long childId);

    /** 회원이 부모 또는 자녀로 속한 방. 목록 조회는 findByParentIdOrChildId(myId, myId) 로 부른다. */
    List<ChatRoom> findByParentIdOrChildId(Long parentId, Long childId);
}
