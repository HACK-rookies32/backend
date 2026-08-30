-- ------------------------------------------------------------------
-- V14. 알림 유형 CHECK 제약에 CHAT_MESSAGE / SCREEN_TIME_LIMIT 추가.
--
-- #152(채팅 알림 발행 + 자녀 하루 스크린타임 한도)가 NotificationType 에
-- CHAT_MESSAGE, SCREEN_TIME_LIMIT 을 추가했지만 V9 의 CHECK 제약은 그대로라
-- 운영(Oracle)에서 채팅 알림 저장이 ORA-02290 으로 실패했다
-- (ChatMessageService.notifyCounterpart WARN, 메시지 자체는 정상 전달).
-- 로컬/CI 는 H2 + ddl-auto 라 제약이 없어 드러나지 않았다.
--
-- enum 에 값을 추가할 때는 이 두 제약을 함께 갱신해야 한다 (V4, V9 참고).
-- ------------------------------------------------------------------
alter table notification drop constraint ck_notification_type;
alter table notification add constraint ck_notification_type check (type in (
    'LOCATION_ENTER','LOCATION_EXIT','LOCATION_ARRIVED','DEVICE_POWER_OFF','HARMFUL_SITE',
    'APP_USAGE','NOTICE','MAINTENANCE','UPDATE','EVENT',
    'PAYMENT_APPROVED','PAYMENT_CANCELED','ALLOWANCE_RECEIVED','ALLOWANCE_RECLAIMED',
    'INQUIRY_ANSWERED','SCREENSHOT_REQUESTED','CHAT_MESSAGE','SCREEN_TIME_LIMIT','ETC'));

alter table notification_setting_toggle drop constraint ck_notification_toggle_type;
alter table notification_setting_toggle add constraint ck_notification_toggle_type check (type in (
    'LOCATION_ENTER','LOCATION_EXIT','LOCATION_ARRIVED','DEVICE_POWER_OFF','HARMFUL_SITE',
    'APP_USAGE','NOTICE','MAINTENANCE','UPDATE','EVENT',
    'PAYMENT_APPROVED','PAYMENT_CANCELED','ALLOWANCE_RECEIVED','ALLOWANCE_RECLAIMED',
    'INQUIRY_ANSWERED','SCREENSHOT_REQUESTED','CHAT_MESSAGE','SCREEN_TIME_LIMIT','ETC'));
