package com.example.kidhackbackend.users.chat.broadcast;

/**
 * STOMP 구독 destination 을 한 곳에서 만든다.
 *
 * 문자열을 발행부와 구독부에 각각 적어 두면 한쪽만 고쳤을 때 조용히 어긋난다.
 * convertAndSend 는 구독자가 0 명이어도 정상 반환이라 그 사고가 에러로 드러나지 않는다.
 */
public final class ChatDestination {

    public static String of(String roomId) {
        return "/sub/chat/" + roomId;
    }

    private ChatDestination() {
    }
}
