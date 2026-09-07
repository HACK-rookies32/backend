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

    // [VULN-DEV-1] childId 를 요청 본문에 적힌 값 그대로 믿는다(본인 검증 없음 — IDOR).
    //   기기 전원/GPS 꺼짐 보고는 "내 기기가 이렇게 됐다"는 신고인데, 누구 기기인지를 신고하는
    //   쪽이 정하게 되어 있다. 로그인한 자녀가 본문에 남의 childId 를 적으면 그대로 기록된다.
    //   → 아래에서 그 자녀의 부모를 찾아 알림을 보내므로, 엉뚱한 집 부모에게 거짓 '트래킹 불가'
    //     알림이 간다. 부모 입장에서는 자녀가 기기를 끈 것처럼 보인다.
    //   호출 횟수 제한(rate limit)도 없어 같은 요청을 반복해 알림을 계속 퍼부을 수 있다.
    //   자녀를 감시하는 앱에서 이런 거짓 신호가 반복되면 부모가 알림 자체를 신뢰하지 않게 되는데,
    //   그러면 정작 진짜 이벤트도 묻힌다는 점이 이 결함의 실질적인 피해다.
    //   방어: 인증 주체(@AuthenticationPrincipal)의 id 를 쓰고 본문 childId 는 무시 + 호출 빈도 제한.
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