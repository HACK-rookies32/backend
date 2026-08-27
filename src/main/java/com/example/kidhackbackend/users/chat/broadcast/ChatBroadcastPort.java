package com.example.kidhackbackend.users.chat.broadcast;

import com.example.kidhackbackend.users.chat.dto.ChatMessageResponse;

/**
 * 저장이 끝난 채팅 메시지를 구독자에게 실어 보내는 경로.
 *
 * 구현이 둘인 이유는 WAS 가 2 대이기 때문이다. STOMP 의 SimpleBroker 는 브로커
 * 프로세스가 아니라 각 JVM 힙 안의 구독자 목록이라, 같은 방의 두 사람이 서로 다른
 * 인스턴스에 붙으면 한쪽은 메시지를 받지 못한다. 운영에서는 Redis pub/sub 을 한 번
 * 거쳐 모든 인스턴스가 같은 사실을 알게 한다.
 *
 * app.chat.broker 로 고른다 (app.push.provider + PushPort 와 같은 방식).
 *   local : 지금까지의 동작. 자기 JVM 안에서만 전달한다. 로컬 개발/CI 기본값.
 *   redis : Redis 채널로 발행하고, 각 인스턴스의 구독자가 자기 소켓에 뿌린다.
 */
public interface ChatBroadcastPort {

    void broadcast(String roomId, ChatMessageResponse message);
}
