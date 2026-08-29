package com.example.kidhackbackend.users.device.service;

import com.example.kidhackbackend.users.device.entity.DeviceEvent;
import com.example.kidhackbackend.users.device.entity.DeviceEventType;
import com.example.kidhackbackend.users.device.repository.DeviceEventRepository;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기기 전원/GPS 감지 처리 — 역할표 No.4.
 *
 * <p>앱이 전원/GPS 꺼짐을 보고하면 기록하고 '트래킹 불가' 알림을 발행한다.
 * (앱 강제종료 방지는 모바일 영역이며, 백엔드는 이벤트 수신·알림만 담당한다.)
 */
@Service
@RequiredArgsConstructor
public class DeviceEventService {

    private final DeviceEventRepository repository;
    private final NotificationPort notificationPort;
    private final MemberRepository memberRepository;

    // [VULN-DEV-1] childId를 요청값 그대로 신뢰(본인 검증 없음)한다.
    //   → 타인 childId로 위조 이벤트를 보내 거짓 '트래킹 불가' 알림을 유발할 수 있다(알림 스팸/오도).
    @Transactional
    public DeviceEvent report(Long childId, String deviceId, DeviceEventType type) {
        DeviceEvent event = repository.save(new DeviceEvent(childId, deviceId, type));

        String reason = switch (type) {
            case POWER_OFF -> "기기 전원 꺼짐";
            case GPS_OFF -> "GPS(위치) 꺼짐";
        };
        // 트래킹이 끊긴 사실을 알아야 하는 쪽은 부모다. 예전에는 수신자가 자녀라
        // 부모 알림 목록에 뜨지 않았다. 미연결 자녀면 보낼 곳이 없어 건너뛴다.
        memberRepository.findById(childId)
                .map(Member::getParent)
                .ifPresent(parent -> notificationPort.publish(parent.getId(), childId,
                        NotificationType.DEVICE_POWER_OFF,
                        "트래킹 불가: " + reason + " 감지"));

        return event;
    }
}