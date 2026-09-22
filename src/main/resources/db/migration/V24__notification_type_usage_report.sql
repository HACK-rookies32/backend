-- ------------------------------------------------------------------
-- V24. 알림 유형 CHECK 제약에 USAGE_REPORT_REQUESTED 추가.
--
-- NotificationType 에 USAGE_REPORT_REQUESTED(사용량 즉시 보고 요청)가 추가됐지만
-- V14 의 CHECK 제약에는 반영되지 않아, 운영(Oracle)에서
-- POST /api/monitoring/usage/{childId}/request 의 알림 저장이 ORA-02290 으로 실패했다
-- (DataIntegrityViolationException -> 400 INVALID_PARAMETER). 부모의 "즉시 보고 요청"
-- 기능이 전면 비동작. 로컬/CI 는 H2 + ddl-auto 라 제약이 없어 드러나지 않았다.
--
-- enum 에 값을 추가할 때는 이 두 제약을 함께 갱신해야 한다 (V4, V9, V14 참고).
-- ------------------------------------------------------------------
alter table notification drop constraint ck_notification_type;
alter table notification add constraint ck_notification_type check (type in (
    'LOCATION_ENTER','LOCATION_EXIT','LOCATION_ARRIVED','DEVICE_POWER_OFF','HARMFUL_SITE',
    'APP_USAGE','NOTICE','MAINTENANCE','UPDATE','EVENT',
    'PAYMENT_APPROVED','PAYMENT_CANCELED','ALLOWANCE_RECEIVED','ALLOWANCE_RECLAIMED',
    'INQUIRY_ANSWERED','SCREENSHOT_REQUESTED','USAGE_REPORT_REQUESTED',
    'CHAT_MESSAGE','SCREEN_TIME_LIMIT','ETC'));

alter table notification_setting_toggle drop constraint ck_notification_toggle_type;
alter table notification_setting_toggle add constraint ck_notification_toggle_type check (type in (
    'LOCATION_ENTER','LOCATION_EXIT','LOCATION_ARRIVED','DEVICE_POWER_OFF','HARMFUL_SITE',
    'APP_USAGE','NOTICE','MAINTENANCE','UPDATE','EVENT',
    'PAYMENT_APPROVED','PAYMENT_CANCELED','ALLOWANCE_RECEIVED','ALLOWANCE_RECLAIMED',
    'INQUIRY_ANSWERED','SCREENSHOT_REQUESTED','USAGE_REPORT_REQUESTED',
    'CHAT_MESSAGE','SCREEN_TIME_LIMIT','ETC'));
