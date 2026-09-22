package com.example.kidhackbackend.users.chat.entity;

/** 채팅 메시지 종류. TEXT 는 본문만, IMAGE/AUDIO 는 mediaUrl 첨부를 동반한다. */
public enum MessageType {
    TEXT,
    IMAGE,
    AUDIO
}
