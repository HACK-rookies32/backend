-- ==================================================================
-- V6 : 감사로그 활동 유형에 관리자 로그아웃(ADMIN_LOGOUT) 추가.
--
-- V2 에서 admin_audit_log.activity_type 을 check 제약으로 열거해 두었기 때문에,
-- AdminActivityType 에 값을 추가하는 것만으로는 운영(Oracle)에서 INSERT 가 제약 위반으로 실패한다.
-- (로컬/CI 는 H2 + ddl-auto 라 제약이 엔티티에서 다시 만들어져 티가 나지 않는다 — 놓치기 쉬운 지점)
--
-- 기존 제약을 떨어뜨리고 ADMIN_LOGOUT 을 포함해 다시 만든다.
-- ⚠️ V2 파일은 이미 적용된 이력이 있으므로 절대 수정하지 않는다(체크섬 불일치 시 앱이 뜨지 않음).
-- ==================================================================

-- V2 에서 인라인 check 로 만들어진 제약은 시스템이 이름을 자동 부여(SYS_Cxxxxxx)하므로
-- 이름으로 지목해 떨어뜨릴 수 없다. activity_type 을 대상으로 하는 check 제약을 찾아 동적으로 제거한다.
declare
    v_name user_constraints.constraint_name%type;
begin
    select constraint_name into v_name
    from user_constraints
    where table_name = 'ADMIN_AUDIT_LOG'
      and constraint_type = 'C'
      and search_condition_vc like '%ACTIVITY_TYPE%'
      and rownum = 1;

    execute immediate 'alter table admin_audit_log drop constraint ' || v_name;
exception
    when no_data_found then
        null;   -- 제약이 없으면(이미 정리됨) 그냥 진행한다.
end;
/

alter table admin_audit_log add constraint ck_admin_audit_log_activity
    check (activity_type in (
        'ADMIN_LOGIN','ADMIN_LOGOUT',
        'MEMBER_UPDATE','MEMBER_DELETE','MEMBER_ROLE_UPDATE','MEMBER_BAN',
        'POST_DELETE','WALLET_BALANCE_ADJUST','PAYMENT_REFUND',
        'INQUIRY_ANSWER_CREATE','INQUIRY_ANSWER_UPDATE','INQUIRY_ANSWER_DELETE',
        'NOTICE_CREATE','NOTICE_UPDATE','NOTICE_DELETE',
        'NOTIFICATION_BROADCAST','HARMFUL_KEYWORD_ADD','HARMFUL_KEYWORD_DELETE'));
