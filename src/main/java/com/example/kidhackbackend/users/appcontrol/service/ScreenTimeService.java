package com.example.kidhackbackend.users.appcontrol.service;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.appcontrol.dto.ScreenTimeResponse;
import com.example.kidhackbackend.users.appcontrol.entity.ScreenTimeSetting;
import com.example.kidhackbackend.users.appcontrol.repository.ScreenTimeSettingRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 자녀 하루 스크린타임 한도 — 조회·설정과 한도 도달 알림.
 *
 * <p>한도 도달 판정은 <b>자녀 앱</b>이 한다. 사용량을 실측하는 쪽이 앱이고, 서버가 집계로
 * 판정하려면 주기 배치가 필요한데 그 집계도 결국 앱이 올린 값이라 신뢰 수준은 같으면서
 * 지연만 늘어난다. 서버는 보고를 받아 알림만 발행한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreenTimeService {

    private final ScreenTimeSettingRepository repository;
    private final MemberRepository memberRepository;
    private final NotificationPort notificationPort;

    /** 설정한 적이 없으면 "제한 없음"으로 답한다. 빈 행을 만들지는 않는다. */
    public ScreenTimeResponse get(Long childId) {
        return repository.findByChildId(childId)
                .map(ScreenTimeResponse::from)
                .orElseGet(() -> ScreenTimeResponse.unlimited(childId));
    }

    /** 한도 설정. {@code null} 이면 제한을 푼다. */
    @Transactional
    public ScreenTimeResponse set(Long childId, Integer dailyLimitMinutes) {
        ScreenTimeSetting setting = repository.findByChildId(childId)
                .orElseGet(() -> new ScreenTimeSetting(childId, null));
        setting.setDailyLimitMinutes(dailyLimitMinutes);
        return ScreenTimeResponse.from(repository.save(setting));
    }

    /**
     * 자녀 앱이 "오늘 한도에 도달했다"고 보고하면 알림을 발행한다.
     *
     * <p>부모와 자녀 <b>양쪽에</b> 보낸다. 자녀도 왜 앱이 막히는지 알아야 하기 때문이다.
     * 부모와 연결되지 않은 자녀면 부모 알림은 건너뛴다.
     *
     * <p>한도가 설정돼 있지 않으면(제한 없음) 보고를 무시한다 — 앱이 낡은 한도를 들고
     * 보고하는 경우를 막는다.
     *
     * @return 알림을 실제로 발행했는지
     */
    @Transactional
    public boolean reportLimitReached(Long childId, int usedMinutes) {
        Integer limit = repository.findByChildId(childId)
                .map(ScreenTimeSetting::getDailyLimitMinutes)
                .orElse(null);
        if (limit == null) {
            return false;
        }

        String message = "오늘 사용 시간이 한도(" + limit + "분)에 도달했어요. 현재 " + usedMinutes + "분 사용.";
        notificationPort.publish(childId, childId, NotificationType.SCREEN_TIME_LIMIT, message);

        memberRepository.findById(childId)
                .map(Member::getParent)
                .ifPresent(parent -> notificationPort.publish(parent.getId(), childId,
                        NotificationType.SCREEN_TIME_LIMIT, message));
        return true;
    }
}
