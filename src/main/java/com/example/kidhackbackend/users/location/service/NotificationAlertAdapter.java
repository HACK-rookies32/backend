package com.example.kidhackbackend.users.location.service;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    /**
     * 자녀의 진입/이탈/도착을 <b>부모에게</b> 알린다.
     *
     * <p>예전에는 수신자를 자녀로 두고 있어(최소 구현) 정작 부모의 알림 목록에는 아무것도
     * 뜨지 않았다. 자녀를 여럿 둔 보호자가 어느 자녀 건지 구분할 수 있도록 childId 도 함께 남긴다.
     * 아직 부모와 연결되지 않은 자녀면 보낼 곳이 없으므로 건너뛴다.
     */
    @Override
    public void sendLocationAlert(Long childId, String type, String message) {
        memberRepository.findById(childId)
                .map(Member::getParent)
                .ifPresent(parent ->
                        notificationPort.publish(parent.getId(), childId, mapType(type), message));
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
