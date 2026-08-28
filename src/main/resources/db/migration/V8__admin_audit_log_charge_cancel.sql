-- ==================================================================
-- V8 : 감사로그에 충전 취소(CHARGE_CANCEL) 추가 + 활동 사유 컬럼(description) 신설.
--
-- 이슈 #139 (프론트 3차 요청 1·4번). 두 가지를 한 파일에서 처리한다.
--   ① admin_audit_log.activity_type 의 check 제약에 CHARGE_CANCEL 을 넣는다.
--   ② 관리자가 입력한 취소 사유를 담을 description 컬럼을 추가한다.
--
-- ⚠️ 왜 마이그레이션이 짝으로 따라와야 하는가
--   로컬·CI 는 H2 + ddl-auto:update 라 엔티티만 고쳐도 스키마가 자동으로 따라온다(증상이 안 보인다).
--   운영은 Oracle + Flyway + ddl-auto:validate 라, 여기서 빠뜨리면 CHARGE_CANCEL INSERT 가 제약
--   위반으로 실패하고 description 컬럼이 없어 앱 기동 자체가 실패한다.
--   실제로 #134 에서 pg_webhook_event 를 빠뜨려 운영 WAS 2대가 크래시(502)한 전례가 있다.
--
-- ⚠️ V7 은 팀원이 선점했다(V7__pg_webhook_event.sql, #136). 그래서 이 파일이 V8 이다.
-- ⚠️ 이미 적용된 V1~V7 은 절대 수정하지 않는다 — 체크섬이 어긋나면 앱이 뜨지 않는다.
-- ==================================================================

-- ------------------------------------------------------------------
-- ① 활동 유형 제약 갱신 — CHARGE_CANCEL 추가
--
-- V6(관리자 로그아웃, #129)에서 제약에 ck_admin_audit_log_activity 라는 이름을 붙여 두었다.
-- 그래서 V6 처럼 user_constraints 를 뒤져 SYS_Cxxxxxx 를 찾을 필요 없이 이름으로 바로 지목한다.
-- 다만 제약이 없는 환경(신규 구축 등)에서도 멈추지 않도록 예외를 삼킨다.
-- ------------------------------------------------------------------
declare
    constraint_not_found exception;
    pragma exception_init(constraint_not_found, -2443);   -- ORA-02443: 제약조건이 존재하지 않음
begin
    execute immediate 'alter table admin_audit_log drop constraint ck_admin_audit_log_activity';
exception
    when constraint_not_found then
        null;
end;
/

alter table admin_audit_log add constraint ck_admin_audit_log_activity
    check (activity_type in (
        'ADMIN_LOGIN','ADMIN_LOGOUT',
        'MEMBER_UPDATE','MEMBER_DELETE','MEMBER_ROLE_UPDATE','MEMBER_BAN',
        'POST_DELETE','WALLET_BALANCE_ADJUST','PAYMENT_REFUND','CHARGE_CANCEL',
        'INQUIRY_ANSWER_CREATE','INQUIRY_ANSWER_UPDATE','INQUIRY_ANSWER_DELETE',
        'NOTICE_CREATE','NOTICE_UPDATE','NOTICE_DELETE',
        'NOTIFICATION_BROADCAST','HARMFUL_KEYWORD_ADD','HARMFUL_KEYWORD_DELETE'));

-- ------------------------------------------------------------------
-- ② 활동 사유 컬럼 추가
--
-- nullable 이다 — 사유를 받지 않는 활동이 대부분이고, 이미 쌓인 이력도 전부 null 로 남아야 한다.
-- 길이 500 은 관리자가 민원 내용을 한두 문장 적는 것을 상정한 값이다.
-- 컬럼명 description 은 Oracle 예약어가 아니다(OracleReservedWordColumnTest 규칙 준수).
-- ------------------------------------------------------------------
alter table admin_audit_log add description varchar2(500 char);
