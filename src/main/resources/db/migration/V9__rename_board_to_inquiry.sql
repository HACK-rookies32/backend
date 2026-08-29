-- ==================================================================
-- V9 : 1:1 문의 도메인 네이밍 통일 (board -> inquiry) + 첨부 다건화 + 답변 알림 유형 추가.
--
-- ⚠️ 원래 V8 로 만들었으나, 같은 시점에 머지된 #141(V8__admin_audit_log_charge_cancel.sql)과
--    버전 번호가 충돌해 Flyway 가 "more than one migration with version 8" 로 기동을 거부했다.
--    (CI 는 H2 + flyway.enabled=false 라 못 잡음) 그래서 V9 로 옮긴다.
--
-- 배경
--   회원용 코드가 전부 board* 네이밍이었는데(테이블 board, FK board_id),
--   관리자용은 inquiry* 였다. 엔드포인트도 /api/boards ↔ /api/admin/inquiries 로
--   갈려 있었다. 회원용을 inquiry 로 맞추면서 스키마도 함께 정리한다.
--
-- 이 파일이 하는 일
--   (1) board                -> inquiry            테이블 리네임
--   (2) inquiry_answer.board_id -> inquiry_id      컬럼 리네임(FK 는 따라옴)
--   (3) inquiry_attachment 테이블 신설 + 기존 attachment_url 값 이관 후 컬럼 제거
--       (단일 String -> List<String>, 커뮤니티 community_post_attachment 와 같은 모양)
--   (4) notification / notification_setting_toggle 의 type CHECK 제약 갱신
--       - INQUIRY_ANSWERED 추가(이번 커밋의 신규 알림 유형)
--       - ALLOWANCE_RECEIVED / ALLOWANCE_RECLAIMED 도 함께 채운다. 코드 enum 에는
--         있었지만 V4 갱신 때 빠져 운영 CHECK 에 없던 값이다(이체 알림 시 ORA-02290 위험).
--
-- 로컬/CI 는 H2 + ddl-auto 라 이 파일을 타지 않는다. 운영(Oracle 19)만 실행된다.
-- ⚠️ V1~V8 은 이미 적용 이력이 있으므로 수정하지 않는다.
-- ==================================================================

-- (1) 테이블 리네임. member_id FK(FKsds8ox89wwf6aihinar49rmfy)는 이름 그대로 inquiry 에 붙어 있다.
alter table board rename to inquiry;

-- (2) 답변 FK 컬럼 리네임. Oracle 은 FK 가 걸린 컬럼도 rename 가능하고 제약이 따라온다.
alter table inquiry_answer rename column board_id to inquiry_id;

-- (3) 첨부 다건 테이블. community_post_attachment 와 동일한 모양(PK 없음, FK + 인덱스).
create table inquiry_attachment (
    inquiry_id number(19,0) not null,
    url        varchar2(255 char)
);

alter table inquiry_attachment
    add constraint fk_inquiry_attachment_inquiry foreign key (inquiry_id) references inquiry;

create index idx_inquiry_attachment_inquiry on inquiry_attachment (inquiry_id);

-- 기존 단일 첨부값을 새 테이블로 옮긴 뒤 컬럼을 떼어낸다.
insert into inquiry_attachment (inquiry_id, url)
select id, attachment_url from inquiry where attachment_url is not null;

alter table inquiry drop column attachment_url;

-- ------------------------------------------------------------------
-- (4) 알림 유형 CHECK 제약 갱신.
--     V4 가 ck_notification_type / ck_notification_toggle_type 로 이름을 붙여 두었으므로
--     탐색 없이 이름으로 바로 드롭/추가한다(V4 파일 주석 참고).
-- ------------------------------------------------------------------
alter table notification drop constraint ck_notification_type;
alter table notification add constraint ck_notification_type check (type in (
    'LOCATION_ENTER','LOCATION_EXIT','LOCATION_ARRIVED','DEVICE_POWER_OFF','HARMFUL_SITE',
    'APP_USAGE','NOTICE','MAINTENANCE','UPDATE','EVENT',
    'PAYMENT_APPROVED','PAYMENT_CANCELED','ALLOWANCE_RECEIVED','ALLOWANCE_RECLAIMED',
    'INQUIRY_ANSWERED','SCREENSHOT_REQUESTED','ETC'));

alter table notification_setting_toggle drop constraint ck_notification_toggle_type;
alter table notification_setting_toggle add constraint ck_notification_toggle_type check (type in (
    'LOCATION_ENTER','LOCATION_EXIT','LOCATION_ARRIVED','DEVICE_POWER_OFF','HARMFUL_SITE',
    'APP_USAGE','NOTICE','MAINTENANCE','UPDATE','EVENT',
    'PAYMENT_APPROVED','PAYMENT_CANCELED','ALLOWANCE_RECEIVED','ALLOWANCE_RECLAIMED',
    'INQUIRY_ANSWERED','SCREENSHOT_REQUESTED','ETC'));
