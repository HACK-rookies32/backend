-- ==================================================================
-- V4 : 공지 첨부·링크 다중화 + 알림 제목 + 알림 유형 3종 추가.
--
-- 배경 : 관리자 웹은 「공지 작성」과 「전체 알림 발송」을 별도 버튼으로 둔다.
--   공지를 써도 알림은 나가지 않고, 알림은 유형/제목/본문을 직접 입력해 보낸다.
--   백엔드가 그 계약에 맞춰지면서 스키마도 따라온다.
--
-- 한 번 적용된 뒤에는 이 파일을 고치지 말 것 — 체크섬이 어긋나면 앱이 뜨지 않는다.
-- ==================================================================

-- ------------------------------------------------------------------
-- 1) 공지 첨부·링크
--
-- 커뮤니티 게시글(community_post_attachment / community_post_link)과 같은 모양이다.
-- @ElementCollection 이라 join 컬럼 하나 + 값 컬럼 하나뿐이고 PK 가 없다.
-- ------------------------------------------------------------------
create table notice_attachment (notice_id number(19,0) not null, url varchar2(255 char));
create table notice_link       (notice_id number(19,0) not null, url varchar2(255 char));

-- 공지 하나의 첨부를 통째로 읽는 것이 유일한 접근 패턴이다.
create index idx_notice_attachment_notice on notice_attachment (notice_id);
create index idx_notice_link_notice       on notice_link (notice_id);

-- notice.attachment_url / link_url 은 일부러 남긴다.
-- Hibernate validate 는 여분 컬럼을 문제 삼지 않고, 기존 공지의 값을 옮길 시간을 번다.

-- ------------------------------------------------------------------
-- 2) 알림 제목
--
-- 단건 알림은 유형이 정한 제목을, 관리자 전체 알림은 관리자가 입력한 제목을 넣는다.
-- 이 컬럼이 생기기 전에 쌓인 행이 있을 수 있어 nullable 이다.
-- ------------------------------------------------------------------
alter table notification add title varchar2(255 char);

-- ------------------------------------------------------------------
-- 3) 알림 유형 3종 추가 (MAINTENANCE / UPDATE / EVENT)
--
-- ⚠️ V1 이 인라인 check 로 만들어 제약 이름이 시스템 생성(SYS_Cxxxx)이다. 이름을 알 수
--    없으므로 조건문으로 찾아서 드롭한 뒤, 이번에는 이름을 붙여 다시 만든다. 다음에
--    유형을 추가할 때는 이 탐색 과정 없이 바로 드롭/추가할 수 있다.
--
--    search_condition_vc 는 Oracle 12c+ 컬럼이다(운영 19.32). 로컬/CI 는 H2 + Flyway
--    비활성이라 이 파일을 타지 않는다.
-- ------------------------------------------------------------------
BEGIN
  FOR c IN (SELECT constraint_name FROM user_constraints
             WHERE table_name = 'NOTIFICATION'
               AND constraint_type = 'C'
               AND search_condition_vc LIKE '%LOCATION_ENTER%') LOOP
    EXECUTE IMMEDIATE 'ALTER TABLE notification DROP CONSTRAINT ' || c.constraint_name;
  END LOOP;

  FOR c IN (SELECT constraint_name FROM user_constraints
             WHERE table_name = 'NOTIFICATION_SETTING_TOGGLE'
               AND constraint_type = 'C'
               AND search_condition_vc LIKE '%LOCATION_ENTER%') LOOP
    EXECUTE IMMEDIATE 'ALTER TABLE notification_setting_toggle DROP CONSTRAINT ' || c.constraint_name;
  END LOOP;
END;
/

alter table notification add constraint ck_notification_type check (type in (
    'LOCATION_ENTER','LOCATION_EXIT','LOCATION_ARRIVED','DEVICE_POWER_OFF','HARMFUL_SITE',
    'APP_USAGE','NOTICE','MAINTENANCE','UPDATE','EVENT',
    'PAYMENT_APPROVED','PAYMENT_CANCELED','SCREENSHOT_REQUESTED','ETC'));

alter table notification_setting_toggle add constraint ck_notification_toggle_type check (type in (
    'LOCATION_ENTER','LOCATION_EXIT','LOCATION_ARRIVED','DEVICE_POWER_OFF','HARMFUL_SITE',
    'APP_USAGE','NOTICE','MAINTENANCE','UPDATE','EVENT',
    'PAYMENT_APPROVED','PAYMENT_CANCELED','SCREENSHOT_REQUESTED','ETC'));
