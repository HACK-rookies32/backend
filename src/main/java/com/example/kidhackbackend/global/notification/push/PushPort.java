package com.example.kidhackbackend.global.notification.push;

import java.util.List;
import java.util.Map;

/**
 * 푸시 발송 창구. 구현체는 {@link PushConfig} 가 {@code app.push.provider} 를 보고 고른다.
 *
 * <p>구현체는 예외를 던지지 않는다. 발송 실패는 {@link PushResult} 로 돌려준다. 호출부가
 * 트랜잭션 안이라, 발송 실패가 예외로 새어 나가면 알림과 무관한 업무(결제 승인 등)까지
 * 함께 롤백되기 때문이다.
 */
public interface PushPort {

    /** 단말 하나에 보낸다. */
    PushResult sendPush(String deviceToken, PushMessage message);

    /**
     * 여러 단말에 같은 내용을 보낸다(공지 전체발송 등).
     *
     * @return 토큰별 결과. 넘긴 토큰이 모두 키로 들어 있다.
     */
    Map<String, PushResult> sendMulticast(List<String> deviceTokens, PushMessage message);
}