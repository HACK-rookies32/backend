package com.example.kidhackbackend.users.chat.broadcast;

import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;

/**
 * Redis 채널에 실려 가는 봉투.
 *
 * 채널을 방마다 파지 않고 하나로 두는 대신 roomId 를 여기에 담는다. 방이 생길 때마다
 * 구독을 동적으로 등록/해제할 필요가 없어진다.
 */
public record ChatBroadcast(String roomId, ChatMessageResponse message) {
}
