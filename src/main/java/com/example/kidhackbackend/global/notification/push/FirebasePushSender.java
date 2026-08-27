package com.example.kidhackbackend.global.notification.push;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 실제 FCM 발송.
 *
 * <p>{@link FirebaseMessaging} 을 생성자로 받는다. 예전에는 {@code getInstance()} 를 매번
 * 호출했는데, 초기화가 안 된 상태면 {@code IllegalStateException}(unchecked)이 튀어나와
 * 알림과 무관한 트랜잭션까지 롤백시켰다. 지금은 {@link PushConfig} 가 초기화에 성공했을
 * 때만 이 객체를 만든다.
 */
@Slf4j
@RequiredArgsConstructor
public class FirebasePushSender implements PushPort {

    /** FCM 멀티캐스트 1회 상한. 넘기면 요청 자체가 거부된다. */
    private static final int MULTICAST_CHUNK_SIZE = 500;

    private final FirebaseMessaging messaging;

    @Override
    public PushResult sendPush(String deviceToken, PushMessage message) {
        try {
            messaging.send(com.google.firebase.messaging.Message.builder()
                    .setToken(deviceToken)
                    .setNotification(toNotification(message))
                    .putAllData(message.data())
                    .setAndroidConfig(highPriority())
                    .build());
            return PushResult.SUCCESS;
        } catch (FirebaseMessagingException e) {
            return classify(e, deviceToken);
        }
    }

    @Override
    public Map<String, PushResult> sendMulticast(List<String> deviceTokens, PushMessage message) {
        Map<String, PushResult> results = new LinkedHashMap<>();
        for (int start = 0; start < deviceTokens.size(); start += MULTICAST_CHUNK_SIZE) {
            List<String> chunk = deviceTokens.subList(
                    start, Math.min(start + MULTICAST_CHUNK_SIZE, deviceTokens.size()));
            results.putAll(sendChunk(chunk, message));
        }
        return results;
    }

    private Map<String, PushResult> sendChunk(List<String> chunk, PushMessage message) {
        Map<String, PushResult> results = new LinkedHashMap<>();
        try {
            BatchResponse batch = messaging.sendEachForMulticast(MulticastMessage.builder()
                    .addAllTokens(chunk)
                    .setNotification(toNotification(message))
                    .putAllData(message.data())
                    .setAndroidConfig(highPriority())
                    .build());

            List<SendResponse> responses = batch.getResponses();
            for (int i = 0; i < chunk.size(); i++) {
                SendResponse response = responses.get(i);
                results.put(chunk.get(i), response.isSuccessful()
                        ? PushResult.SUCCESS
                        : classify(response.getException(), chunk.get(i)));
            }
        } catch (FirebaseMessagingException e) {
            // 청크 전체가 실패한 경우(인증·네트워크). 개별 토큰 문제가 아니므로
            // TOKEN_INVALID 로 오해해서 멀쩡한 토큰을 지우면 안 된다.
            log.warn("FCM 멀티캐스트 실패: {} 건, error={}", chunk.size(), e.getMessage());
            chunk.forEach(token -> results.put(token, PushResult.FAILED));
        }
        return results;
    }

    /**
     * 실패 원인을 "토큰이 죽었다" 와 "그 밖의 실패" 로 나눈다.
     *
     * <p>{@code INVALID_ARGUMENT} 는 일부러 뺐다. 토큰 형식 오류에도 나오지만 <b>페이로드가
     * 잘못됐을 때도</b> 나온다. 후자라면 모든 토큰이 이 코드로 실패하는데, 그걸 토큰 문제로
     * 보면 사용자 전원의 토큰을 지워 버린다. 쓰레기 토큰이 남는 편이 훨씬 낫다.
     */
    private PushResult classify(FirebaseMessagingException e, String deviceToken) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
            log.info("FCM 무효 토큰: code={}, token={}", code, deviceToken);
            return PushResult.TOKEN_INVALID;
        }
        log.warn("FCM 발송 실패: code={}, token={}, error={}", code, deviceToken, e.getMessage());
        return PushResult.FAILED;
    }

    private Notification toNotification(PushMessage message) {
        return Notification.builder()
                .setTitle(message.title())
                .setBody(message.body())
                .build();
    }

    /**
     * 절전(Doze) 상태의 단말에도 바로 도달하게 한다. 위치 이탈·유해사이트 감지처럼
     * 늦게 오면 의미가 없는 알림이 대부분이다.
     */
    private AndroidConfig highPriority() {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();
    }
}