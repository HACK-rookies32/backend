package com.example.kidhackbackend.global.notification.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.notification.dto.NotificationPageResponse;
import com.example.kidhackbackend.global.notification.dto.NotificationResponse;
import com.example.kidhackbackend.global.notification.entity.Notification;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.push.PushDispatcher;
import com.example.kidhackbackend.global.notification.push.PushMessage;
import com.example.kidhackbackend.global.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 알림 저장 + 푸시 발행(공통 도메인).
 *
 * <p><b>발송 순서:</b> 알림 행을 트랜잭션으로 저장·커밋한 뒤, 트랜잭션 밖에서
 * {@link PushDispatcher} 로 FCM 을 호출한다. 예전에는 발송이 호출부(결제 승인·용돈
 * 이체 등)의 트랜잭션에 얹혀, FCM 응답을 기다리는 동안 DB 커넥션을 잡고 있었다.
 * 그래서 {@code @Transactional} 대신 {@link TransactionTemplate} 로 저장 구간만 감싼다.
 */
@Slf4j
@Service
public class NotificationService implements NotificationPort {

    private final NotificationRepository repository;
    private final NotificationSettingService notificationSettingService;
    private final PushDispatcher pushDispatcher;
    private final TransactionTemplate txTemplate;

    public NotificationService(NotificationRepository repository,
                               NotificationSettingService notificationSettingService,
                               PushDispatcher pushDispatcher,
                               PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.notificationSettingService = notificationSettingService;
        this.pushDispatcher = pushDispatcher;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void publish(Long recipientId, NotificationType type, String message) {
        PushMessage push = txTemplate.execute(status -> {
            if (!notificationSettingService.isEnabled(recipientId, type)) {
                log.info("알림 설정이 꺼져있어 발송 생략: recipientId={}, type={}", recipientId, type);
                return null;
            }
            Notification saved = repository.save(new Notification(recipientId, type, message));
            return toPushMessage(saved);
        });

        if (push == null) {
            return; // 설정 꺼짐
        }
        pushDispatcher.deliverToMember(recipientId, push);
    }

    /**
     * 여러 수신자에게 같은 알림을 보낸다(관리자 전체 알림).
     *
     * <p>토큰을 모아 멀티캐스트 한 번으로 보낸다. 알림 저장과 설정(끔) 확인은 수신자별로 한다.
     * 단건과 달리 제목을 인자로 받는다 — 관리자가 "점검 안내" 처럼 직접 적은 제목을 그대로
     * 단말 알림창에 띄워야 하기 때문이다.
     */
    public void publishAll(Collection<Long> recipientIds, NotificationType type,
                           String title, String message) {
        List<Long> targets = txTemplate.execute(status -> {
            List<Long> filtered = recipientIds.stream()
                    .filter(recipientId -> notificationSettingService.isEnabled(recipientId, type))
                    .toList();
            if (!filtered.isEmpty()) {
                repository.saveAll(filtered.stream()
                        .map(recipientId -> new Notification(recipientId, type, title, message))
                        .toList());
            }
            return filtered;
        });

        if (targets == null || targets.isEmpty()) {
            return;
        }
        // 수신자마다 알림 id 가 달라 notificationId 는 싣지 않는다. 앱은 type 으로 목록을 연다.
        pushDispatcher.deliverToMembers(targets, new PushMessage(title, message, Map.of("type", type.name())));
    }

    /**
     * 알림창에 보이는 제목/본문과, 앱이 탭 처리에 쓰는 data 를 만든다.
     *
     * <p>제목은 저장된 값을 그대로 쓴다. 단건 알림은 유형이 정한 제목, 관리자 전체 알림은
     * 관리자가 입력한 제목이다. 유형은 화면에 보이지 않는 data 로만 넘긴다(예전에는 제목
     * 자리에 "PAYMENT_APPROVED" 가 그대로 떴다).
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

    /* ---------------------------------------------------------------- 조회 */

    /** 목록(페이징). 최신순. */
    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(Long recipientId, Pageable pageable) {
        Page<NotificationResponse> page = repository
                .findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(NotificationResponse::from);
        return new NotificationPageResponse(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /** 테스트/내부용 — 페이징 없는 전체 목록. */
    @Transactional(readOnly = true)
    public List<Notification> findByRecipient(Long recipientId) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 알림입니다: " + notificationId));
        notification.markAsRead();
    }

    /** 수신자의 안 읽은 알림을 모두 읽음 처리. 처리된 건수를 반환한다. */
    @Transactional
    public int markAllAsRead(Long recipientId) {
        return repository.markAllRead(recipientId);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long recipientId) {
        return repository.countByRecipientIdAndReadFalse(recipientId);
    }
}
