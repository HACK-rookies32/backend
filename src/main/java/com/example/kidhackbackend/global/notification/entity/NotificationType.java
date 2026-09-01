package com.example.kidhackbackend.global.notification.entity;

/**
 * 알림 유형. 여러 도메인이 공통으로 사용하며 필요 시 값을 확장한다.
 *
 * <p>{@code title} 은 <b>단말 알림창에 보이는 제목</b>이다. 예전에는 푸시 제목 자리에
 * {@code name()} 을 그대로 넣어 사용자에게 "PAYMENT_APPROVED" 가 보였다. 상수 이름은
 * API 응답({@code NotificationResponse.type})에 그대로 실려 나가므로 바꾸지 않는다.
 *
 * <p>대부분은 사용자가 마이페이지에서 끄고 켤 수 있다(명세서 4.2 알림 설정).
 * 그러나 <b>알림처럼 보내지만 실제로는 기기 제어 신호인 유형</b>이 있다. 그런 값까지
 * 토글 대상이 되면 감시받는 쪽이 스스로 감시를 끌 수 있게 되므로
 * {@code userConfigurable = false} 로 표시해 설정에서 제외한다.
 */
public enum NotificationType {
    // 위치 도메인
    LOCATION_ENTER("장소 진입"),     // 지오펜스 진입
    LOCATION_EXIT("장소 이탈"),      // 지오펜스 이탈
    LOCATION_ARRIVED("목적지 도착"), // 목적지 도착

    // 알림 도메인 후속 기능(자리만 확보)
    DEVICE_POWER_OFF("트래킹 불가"), // No.4 전원/GPS 꺼짐
    HARMFUL_SITE("유해 접속 감지"),  // No.5 유해 사이트 감지
    APP_USAGE("앱 사용 알림"),

    // 관리자 전체 알림(POST /api/admin/notifications/broadcast). 관리자 웹이 이 넷 중
    // 하나를 골라 보낸다. 공지 작성과는 별개다 — 공지를 써도 알림은 나가지 않는다.
    NOTICE("공지사항"),
    MAINTENANCE("점검 안내"),
    UPDATE("업데이트 안내"),
    EVENT("이벤트"),

    // 결제 도메인
    PAYMENT_APPROVED("결제 승인"),   // No.44 QR 매장결제 승인(부모 알림)
    PAYMENT_CANCELED("결제 취소"),   // No.46 매장결제 취소(부모 알림)

    // 지갑 이체 도메인 (No.39·40) — 자녀에게 알린다
    ALLOWANCE_RECEIVED("용돈 받음"),     // 부모→자녀 이체
    ALLOWANCE_RECLAIMED("잔액 회수됨"),  // 자녀→부모 회수

    // 1:1 문의 도메인 — 관리자가 답변을 등록하면 문의한 회원(부모)에게 알린다
    INQUIRY_ANSWERED("문의 답변 등록"),

    /**
     * 부모가 자녀 앱에 즉시 촬영을 요청(자녀 기기로 트리거).
     *
     * <p>수신자가 감시 대상인 자녀이고, 내용이 "알림"이 아니라 촬영 명령이다.
     * 토글 대상으로 두면 자녀가 {@code PATCH /api/users/me/notification-settings} 로
     * 이 값을 꺼서 부모의 추가 촬영 요청을 통째로 무력화할 수 있다(감시 회피).
     * 그래서 설정 대상에서 뺀다.
     */
    SCREENSHOT_REQUESTED("화면 캡처 요청", false),

    /**
     * 부모가 자녀 앱에 사용량을 지금 올리라고 요청(자녀 기기로 트리거).
     *
     * <p>자녀 앱은 사용량을 1시간 주기로만 올린다. 부모가 사용 리포트를 새로고침할 때
     * 이 요청을 보내야 방금까지의 사용시간이 반영된다.
     *
     * <p>{@link #SCREENSHOT_REQUESTED} 와 같은 이유로 설정 대상에서 뺀다 — 자녀가 이
     * 값을 꺼서 사용량 보고를 통째로 막을 수 있으면 감시가 무력화된다. 또한 촬영 요청과
     * 달리 자녀 기기에 알림을 띄우지 않는다. 부모가 새로고침할 때마다 자녀 폰이 울린다.
     */
    USAGE_REPORT_REQUESTED("사용량 보고 요청", false),

    // 채팅 — 상대가 보낸 메시지를 앱을 닫아 둔 사이에도 알린다
    CHAT_MESSAGE("새 메시지"),

    /**
     * 자녀의 하루 스크린타임이 부모가 정한 한도에 도달.
     *
     * <p>부모와 자녀 **양쪽에** 발행한다. 자녀도 왜 앱이 막히는지 알아야 하기 때문이다.
     * 판정은 사용량을 실측하는 자녀 앱이 하고 서버는 알림만 낸다
     * ({@code POST /api/appcontrol/screentime/{childId}/reached}).
     */
    SCREEN_TIME_LIMIT("스크린타임 한도 도달"),

    ETC("알림");                     // 기타

    private final String title;
    private final boolean userConfigurable;

    NotificationType(String title) {
        this(title, true);
    }

    NotificationType(String title, boolean userConfigurable) {
        this.title = title;
        this.userConfigurable = userConfigurable;
    }

    /** 단말 알림창에 보이는 제목. */
    public String getTitle() {
        return title;
    }

    /** 사용자가 알림 설정에서 끄고 켤 수 있는 유형인가. */
    public boolean isUserConfigurable() {
        return userConfigurable;
    }
}