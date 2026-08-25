package com.example.kidhackbackend.users.chat.service;

import com.example.kidhackbackend.users.chat.entity.ChatRoom;
import com.example.kidhackbackend.users.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    public ChatRoom getOrCreateRoom(Long parentId, Long childId) {
        return chatRoomRepository.findByParentIdAndChildId(parentId, childId)
                .orElseGet(() -> chatRoomRepository.save(new ChatRoom(parentId, childId)));
    }
}