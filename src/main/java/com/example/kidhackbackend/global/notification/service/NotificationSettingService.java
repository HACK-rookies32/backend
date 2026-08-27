package com.example.kidhackbackend.global.notification.service;

import com.example.kidhackbackend.global.notification.entity.NotificationSetting;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

/**
 * 알림 설정(명세서 4.2).
 *
 * <p>노출·수정 대상은 {@link NotificationType#isUserConfigurable()} 인 유형뿐이다.
 * 제어 신호 계열(예: SCREENSHOT_REQUESTED)은 조회 응답에도 나오지 않고, 요청에
 * 섞여 들어와도 무시하며, 발송 시 설정 검사를 아예 타지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingService {

    private final NotificationSettingRepository repository;

    public Map<NotificationType, Boolean> getSettings(Long memberId) {
        return repository.findByMemberId(memberId)
                .map(setting -> configurableOnly(setting.getToggles()))
                .orElseGet(this::defaultSettings);
    }

    @Transactional
    public Map<NotificationType, Boolean> updateSettings(Long memberId, Map<NotificationType, Boolean> updates) {
        NotificationSetting setting = repository.findByMemberId(memberId)
                .orElseGet(() -> new NotificationSetting(memberId, defaultSettings()));
        setting.update(configurableOnly(updates));
        repository.save(setting);
        return configurableOnly(setting.getToggles());
    }

    /**
     * 특정 유형 알림이 켜져있는지 확인 (NotificationService.publish에서 사용).
     * 설정이 아예 없으면 기본 허용.
     *
     * <p>제어 신호 계열은 사용자 설정과 무관하게 항상 통과한다. 감시 대상이 스스로
     * 트리거를 꺼 버리는 것을 막기 위해서다.
     */
    public boolean isEnabled(Long memberId, NotificationType type) {
        if (!type.isUserConfigurable()) {
            return true;
        }
        return repository.findByMemberId(memberId)
                .map(setting -> setting.isEnabled(type))
                .orElse(true);
    }

    private Map<NotificationType, Boolean> defaultSettings() {
        Map<NotificationType, Boolean> defaults = new EnumMap<>(NotificationType.class);
        for (NotificationType type : NotificationType.values()) {
            if (type.isUserConfigurable()) {
                defaults.put(type, true);
            }
        }
        return defaults;
    }

    /**
     * 사용자가 다룰 수 있는 유형만 남긴다.
     *
     * <p>들어오는 쪽(요청)과 나가는 쪽(응답) 양쪽에 건다. 저장된 행에 예전 값이
     * 남아 있어도 응답에 새어 나가지 않는다.
     */
    private Map<NotificationType, Boolean> configurableOnly(Map<NotificationType, Boolean> source) {
        Map<NotificationType, Boolean> filtered = new EnumMap<>(NotificationType.class);
        source.forEach((type, enabled) -> {
            if (type != null && type.isUserConfigurable() && enabled != null) {
                filtered.put(type, enabled);
            }
        });
        return filtered;
    }
}
