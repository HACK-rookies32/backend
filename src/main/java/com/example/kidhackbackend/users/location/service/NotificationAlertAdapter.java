package com.example.kidhackbackend.users.location.service;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 위치 도메인의 {@link AlertPort}를 알림 공통 도메인으로 잇는 어댑터.
 *
 * <p>위치에서 진입/이탈/도착이 감지되면 이 어댑터가 알림을 실제로 생성한다.
 * 기존 {@link LogAlertAdapter}(로그만) 대신 {@link Primary}로 우선 사용된다.
 */
@Component
@Primary
@RequiredArgsConstructor
public class NotificationAlertAdapter implements AlertPort {

    private final NotificationPort notificationPort;

    @Override
    public void sendLocationAlert(Long childId, String type, String message) {
        // TODO(2단계): childId → 부모(Member) 매핑 후 부모를 수신자로 지정한다.
        //              현재(최소 구현)는 childId를 수신자로 임시 사용한다.
        notificationPort.publish(childId, mapType(type), message);
    }

    private NotificationType mapType(String type) {
        return switch (type) {
            case "ENTER" -> NotificationType.LOCATION_ENTER;
            case "EXIT" -> NotificationType.LOCATION_EXIT;
            case "ARRIVED" -> NotificationType.LOCATION_ARRIVED;
            default -> NotificationType.ETC;
        };
    }
}
