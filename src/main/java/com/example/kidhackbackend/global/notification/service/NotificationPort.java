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

    /**
     * 특정 자녀에 대한 알림을 생성한다.
     *
     * <p>수신자와 대상 자녀는 다를 수 있다(예: 자녀의 결제를 부모에게 알림).
     * 자녀를 여럿 둔 보호자가 목록에서 자녀별로 걸러 볼 수 있게 하는 데 쓴다.
     *
     * <p>{@code default} 로 둔 이유: 이 인터페이스는 테스트 대역 등 여러 곳에서 구현된다.
     * 추상 메서드로 만들면 자녀와 무관한 구현까지 전부 손봐야 하고, 단일 추상 메서드가
     * 아니게 되어 람다로 쓰던 자리도 깨진다. 기본 구현은 자녀 정보를 버리고 기존 경로로
     * 넘긴다 — 실제 저장을 담당하는 {@code NotificationService} 는 이 메서드를 재정의해
     * {@code childId} 를 기록한다.
     *
     * @param recipientId 알림을 받는 사용자
     * @param childId     알림의 대상 자녀 (맥락이 없으면 {@code null})
     * @param type        알림 유형
     * @param message     알림 메시지
     */
    default void publish(Long recipientId, Long childId, NotificationType type, String message) {
        publish(recipientId, type, message);
    }
}
