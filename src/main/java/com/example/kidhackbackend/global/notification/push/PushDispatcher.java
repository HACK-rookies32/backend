package com.example.kidhackbackend.global.notification.push;

import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import com.example.kidhackbackend.global.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 토큰 조회 → 푸시 발송 → 무효 토큰 정리.
 *
 * <p>{@link com.example.kidhackbackend.global.notification.service.NotificationService} 가
 * 알림 <b>저장을 커밋한 뒤</b> 이 빈을 호출한다. FCM 왕복(수 초 가능)을 DB 트랜잭션
 * 밖에서 하기 위한 분리다 — 예전에는 결제 승인 같은 업무 트랜잭션이 FCM 응답을
 * 기다리는 동안 커넥션을 잡고 있었다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushDispatcher {

    private final PushPort pushPort;
    private final DeviceTokenRepository deviceTokenRepository;

    /** 한 회원의 모든 기기에 보낸다(단건 알림). */
    public void deliverToMember(Long memberId, PushMessage message) {
        List<String> tokens = deviceTokenRepository.findByMemberId(memberId).stream()
                .map(DeviceToken::getToken)
                .toList();
        if (tokens.isEmpty()) {
            return;
        }

        List<String> invalid = new ArrayList<>();
        for (String token : tokens) {
            if (safeSend(token, message) == PushResult.TOKEN_INVALID) {
                invalid.add(token);
            }
        }
        purge(invalid);
    }

    /** 여러 회원의 기기에 같은 내용을 멀티캐스트로 보낸다(전체발송). */
    public void deliverToMembers(Collection<Long> memberIds, PushMessage message) {
        List<String> tokens = deviceTokenRepository.findAllByMemberIdIn(memberIds).stream()
                .map(DeviceToken::getToken)
                .toList();
        if (tokens.isEmpty()) {
            return;
        }

        Map<String, PushResult> results;
        try {
            results = pushPort.sendMulticast(tokens, message);
        } catch (RuntimeException e) {
            // PushPort 계약상 나오면 안 되지만, 방어적으로. 토큰은 건드리지 않는다.
            log.warn("멀티캐스트 발송 중 예외 — 토큰 유지: {}", e.toString());
            return;
        }

        purge(results.entrySet().stream()
                .filter(entry -> entry.getValue() == PushResult.TOKEN_INVALID)
                .map(Map.Entry::getKey)
                .toList());
    }

    /**
     * {@link PushPort#sendPush} 는 예외를 던지지 않기로 돼 있으나, 구현체가 FCM 예외가
     * 아닌 unchecked(잘못된 토큰/페이로드 등)를 흘릴 수 있다. 그게 발송 호출부(결제 등)
     * 트랜잭션까지 롤백시키지 않도록 여기서 막고 {@link PushResult#FAILED} 로 취급한다.
     */
    private PushResult safeSend(String token, PushMessage message) {
        try {
            return pushPort.sendPush(token, message);
        } catch (RuntimeException e) {
            log.warn("푸시 발송 중 예외 — 토큰 유지: {}", e.toString());
            return PushResult.FAILED;
        }
    }

    /** FCM 이 "죽은 토큰" 이라고 답한 것만 지운다(네트워크 실패 등은 유지). */
    private void purge(Collection<String> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        deviceTokenRepository.deleteByTokenIn(tokens);
        log.info("무효 기기 토큰 {} 건을 정리했다.", tokens.size());
    }
}
