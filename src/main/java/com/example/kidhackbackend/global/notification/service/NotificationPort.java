package com.example.kidhackbackend.global.notification.service;

import com.example.kidhackbackend.global.notification.entity.NotificationType;

/**
 * 알림 발행 인터페이스(공통).
 *
 * <p>다른 도메인(위치·전원감지·유해사이트 등)은 이 인터페이스를 통해 알림을 생성한다.
 * 최소 시그니처(수신자·유형·메시지)로 시작하며, 팀 요구가 생기면 확장한다.
 */
public interface NotificationPort {

    /**
     * 알림을 생성한다.
     *
     * @param recipientId 알림을 받는 사용자(예: 부모)
     * @param type        알림 유형
     * @param message     알림 메시지
     */
    void publish(Long recipientId, NotificationType type, String message);
}
