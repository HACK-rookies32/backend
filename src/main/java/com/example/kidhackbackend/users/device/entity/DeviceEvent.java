package com.example.kidhackbackend.users.device.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기기 전원/GPS 통제 이벤트 이력 — 역할표 No.4.
 *
 * <p>앱이 전원/GPS 꺼짐을 보고하면 기록하고 '트래킹 불가' 알림을 발행한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "device_event",
        indexes = @Index(name = "idx_device_event_child", columnList = "childId,createdAt")
)
public class DeviceEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long childId;

    /** 기기 식별자 (관리자 페이지의 "등록 기기 목록" 구분용으로 추가) */
    @Column(nullable = false)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceEventType type;

    public DeviceEvent(Long childId, String deviceId, DeviceEventType type) {
        this.childId = childId;
        this.deviceId = deviceId;
        this.type = type;
    }
}
