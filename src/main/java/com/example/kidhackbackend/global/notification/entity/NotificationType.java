package com.example.kidhackbackend.global.notification.entity;

/**
 * 알림 유형. 여러 도메인이 공통으로 사용하며 필요 시 값을 확장한다.
 *
 * <p>대부분은 사용자가 마이페이지에서 끄고 켤 수 있다(명세서 4.2 알림 설정).
 * 그러나 <b>알림처럼 보내지만 실제로는 기기 제어 신호인 유형</b>이 있다. 그런 값까지
 * 토글 대상이 되면 감시받는 쪽이 스스로 감시를 끌 수 있게 되므로
 * {@code userConfigurable = false} 로 표시해 설정에서 제외한다.
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
    NOTICE,

    // 결제 도메인
    PAYMENT_APPROVED,   // No.44 QR 매장결제 승인(부모 알림)
    PAYMENT_CANCELED,   // No.46 매장결제 취소(부모 알림)

    /**
     * 부모가 자녀 앱에 즉시 촬영을 요청(자녀 기기로 트리거).
     *
     * <p>수신자가 감시 대상인 자녀이고, 내용이 "알림"이 아니라 촬영 명령이다.
     * 토글 대상으로 두면 자녀가 {@code PATCH /api/users/me/notification-settings} 로
     * 이 값을 꺼서 부모의 추가 촬영 요청을 통째로 무력화할 수 있다(감시 회피).
     * 그래서 설정 대상에서 뺀다.
     */
    SCREENSHOT_REQUESTED(false),

    ETC;                // 기타

    private final boolean userConfigurable;

    NotificationType() {
        this(true);
    }

    NotificationType(boolean userConfigurable) {
        this.userConfigurable = userConfigurable;
    }

    /** 사용자가 알림 설정에서 끄고 켤 수 있는 유형인가. */
    public boolean isUserConfigurable() {
        return userConfigurable;
    }
}
