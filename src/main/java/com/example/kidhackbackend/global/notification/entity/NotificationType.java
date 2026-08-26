package com.example.kidhackbackend.global.notification.entity;

/**
 * 알림 유형. 여러 도메인이 공통으로 사용하며 필요 시 값을 확장한다.
 */
public enum NotificationType {
    // 위치 도메인
    LOCATION_ENTER,     // 지오펜스 진입
    LOCATION_EXIT,      // 지오펜스 이탈
    LOCATION_ARRIVED,   // 목적지 도착

    // 알림 도메인 후속 기능(자리만 확보)
    DEVICE_POWER_OFF,   // No.4 전원/GPS 꺼짐
    HARMFUL_SITE,       // No.5 유해 사이트 감지
    APP_USAGE,

    // 결제 도메인
    PAYMENT_APPROVED,   // No.44 QR 매장결제 승인(부모 알림)
    PAYMENT_CANCELED,   // No.46 매장결제 취소(부모 알림)

    ETC                 // 기타
}
