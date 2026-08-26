package com.example.kidhackbackend.global.notification.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_setting")
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long memberId;

    @ElementCollection
    @CollectionTable(
            name = "notification_setting_toggle",
            joinColumns = @JoinColumn(name = "notification_setting_id")
    )
    @MapKeyColumn(name = "type")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "enabled", nullable = false)
    private Map<NotificationType, Boolean> toggles = new EnumMap<>(NotificationType.class);

    public NotificationSetting(Long memberId, Map<NotificationType, Boolean> toggles) {
        this.memberId = memberId;
        this.toggles = toggles;
    }

    public boolean isEnabled(NotificationType type) {
        return toggles.getOrDefault(type, true); // 설정 안 한 유형은 기본 허용
    }

    public void update(Map<NotificationType, Boolean> updates) {
        toggles.putAll(updates);
    }
}