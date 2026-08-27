package com.example.kidhackbackend.global.notification.push;

import java.util.Map;

/**
 * 푸시 한 건의 내용.
 *
 * <p>{@code title}/{@code body} 는 단말 알림창에 그대로 보이는 문구다. 예전에는 제목 자리에
 * {@code NotificationType.name()} 이 들어가 사용자 화면에 "PAYMENT_APPROVED" 가 떴다.
 *
 * <p>{@code data} 는 화면에 보이지 않고 앱이 읽는 값이다. 알림을 탭했을 때 어느 화면으로
 * 갈지 결정하는 데 쓰므로 최소한 {@code type} 은 실어 보낸다. FCM 의 data 는 문자열
 * 맵이라 숫자도 문자열로 넣는다.
 */
public record PushMessage(String title, String body, Map<String, String> data) {

    public PushMessage {
        data = (data == null) ? Map.of() : Map.copyOf(data);
    }
}
