package com.example.kidhackbackend.global.notification.service;

import com.example.kidhackbackend.global.notification.entity.DeviceToken;
import com.example.kidhackbackend.global.notification.entity.Notification;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.push.PushMessage;
import com.example.kidhackbackend.global.notification.push.PushPort;
import com.example.kidhackbackend.global.notification.push.PushResult;
import com.example.kidhackbackend.global.notification.repository.DeviceTokenRepository;
import com.example.kidhackbackend.global.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationPort {

    private final NotificationRepository repository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationSettingService notificationSettingService;
    private final PushPort pushPort;

    @Override
    @Transactional
    public void publish(Long recipientId, NotificationType type, String message) {
        if (!notificationSettingService.isEnabled(recipientId, type)) {
            log.info("알림 설정이 꺼져있어 발송 생략: recipientId={}, type={}", recipientId, type);
            return;
        }

        Notification saved = repository.save(new Notification(recipientId, type, message));
        sendPush(saved);
    }

    /**
     * 여러 수신자에게 같은 알림을 보낸다(관리자 전체 알림).
     *
     * <p>예전에는 회원 수만큼 {@link #publish} 를 돌려 FCM 을 한 명씩 호출했다. 여기서는
     * 토큰을 모아 멀티캐스트 한 번으로 보낸다. 알림 저장과 설정(끔) 확인은 기존과 동일하게
     * 수신자별로 한다.
     *
     * <p>단건과 달리 제목을 인자로 받는다. 관리자가 "점검 안내" 처럼 직접 적은 제목을
     * 그대로 단말 알림창에 띄워야 하기 때문이다.
     */
    @Transactional
    public void publishAll(Collection<Long> recipientIds, NotificationType type,
                           String title, String message) {
        List<Long> targets = recipientIds.stream()
                .filter(recipientId -> notificationSettingService.isEnabled(recipientId, type))
                .toList();
        if (targets.isEmpty()) {
            return;
        }

        repository.saveAll(targets.stream()
                .map(recipientId -> new Notification(recipientId, type, title, message))
                .toList());

        List<String> tokens = deviceTokenRepository.findAllByMemberIdIn(targets).stream()
                .map(DeviceToken::getToken)
                .toList();
        if (tokens.isEmpty()) {
            return;
        }

        // 수신자마다 알림 id 가 달라 notificationId 는 싣지 않는다. 앱은 type 으로 목록을 연다.
        PushMessage push = new PushMessage(title, message, Map.of("type", type.name()));
        Map<String, PushResult> results = pushPort.sendMulticast(tokens, push);

        deleteInvalidTokens(results.entrySet().stream()
                .filter(entry -> entry.getValue() == PushResult.TOKEN_INVALID)
                .map(Map.Entry::getKey)
                .toList());
    }

    private void sendPush(Notification notification) {
        deviceTokenRepository.findByMemberId(notification.getRecipientId())
                .ifPresent(deviceToken -> {
                    PushResult result = pushPort.sendPush(deviceToken.getToken(), toPushMessage(notification));
                    if (result == PushResult.TOKEN_INVALID) {
                        deleteInvalidTokens(List.of(deviceToken.getToken()));
                    }
                });
    }

    /**
     * 알림창에 보이는 제목/본문과, 앱이 탭 처리에 쓰는 data 를 만든다.
     *
     * <p>제목에 {@code type.name()} 을 넣던 시절에는 사용자 화면에 "PAYMENT_APPROVED" 가
     * 그대로 떴다. 유형은 눈에 보이지 않는 data 로 넘긴다.
     *
     * <p>제목은 저장된 값을 그대로 쓴다. 단건 알림은 유형이 정한 제목이고, 관리자 전체
     * 알림은 관리자가 입력한 제목이다.
     */
    private PushMessage toPushMessage(Notification notification) {
        return new PushMessage(
                notification.getTitle(),
                notification.getMessage(),
                Map.of(
                        "type", notification.getType().name(),
                        "notificationId", String.valueOf(notification.getId())
                ));
    }

    /**
     * FCM 이 "이 토큰은 죽었다" 고 답한 것만 지운다. 네트워크 실패 같은 일시적 오류로는
     * 지우지 않는다({@link PushResult} 참고).
     */
    private void deleteInvalidTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        deviceTokenRepository.deleteByTokenIn(tokens);
        log.info("무효 기기 토큰 {} 건을 정리했다.", tokens.size());
    }

    @Transactional(readOnly = true)
    public List<Notification> findByRecipient(Long recipientId) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다: " + notificationId));
        notification.markAsRead();
    }

    @Transactional(readOnly = true)
    public long countUnread(Long recipientId) {
        return repository.countByRecipientIdAndReadFalse(recipientId);
    }
}