package com.example.kidhackbackend.global.notification.service;

import com.example.kidhackbackend.global.notification.entity.NotificationSetting;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingService {

    private final NotificationSettingRepository repository;

    public Map<NotificationType, Boolean> getSettings(Long memberId) {
        return repository.findByMemberId(memberId)
                .map(NotificationSetting::getToggles)
                .orElseGet(this::defaultSettings);
    }

    @Transactional
    public Map<NotificationType, Boolean> updateSettings(Long memberId, Map<NotificationType, Boolean> updates) {
        NotificationSetting setting = repository.findByMemberId(memberId)
                .orElseGet(() -> new NotificationSetting(memberId, defaultSettings()));
        setting.update(updates);
        repository.save(setting);
        return setting.getToggles();
    }

    /** 특정 유형 알림이 켜져있는지 확인 (NotificationService.publish에서 사용). 설정이 아예 없으면 기본 허용. */
    public boolean isEnabled(Long memberId, NotificationType type) {
        return repository.findByMemberId(memberId)
                .map(setting -> setting.isEnabled(type))
                .orElse(true);
    }

    private Map<NotificationType, Boolean> defaultSettings() {
        Map<NotificationType, Boolean> defaults = new EnumMap<>(NotificationType.class);
        for (NotificationType type : NotificationType.values()) {
            defaults.put(type, true);
        }
        return defaults;
    }
}