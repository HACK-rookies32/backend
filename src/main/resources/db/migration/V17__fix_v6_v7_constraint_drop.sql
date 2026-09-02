-- ==================================================================
-- V17 : V6 / V7 이 잘못 드롭한 NOT NULL 복구 + 남은 구버전 check 제거.
--
-- 무엇이 잘못됐나
--   V6 / V7 은 V1·V2 가 인라인으로 만든 익명 check 제약(SYS_Cxxxxxx)을
--   찾아 드롭하려고 아래 조건을 썼다.
--
--       where search_condition_vc like '%STATUS%'          -- V7
--       where search_condition_vc like '%ACTIVITY_TYPE%'   -- V6
--
--   그런데 search_condition_vc 는 DDL 에 쓰인 원문을 그대로 보관한다.
--   V1/V2 는 컬럼명을 소문자로 적었으므로 조건식은
--       (status in ('READY','PAID','CANCELLED'))
--   이고 대문자 '%STATUS%' 에 걸리지 않는다.
--   반면 Oracle 이 not null 로 자동 생성한 제약의 조건식은
--       "STATUS" IS NOT NULL
--   로 대문자라 여기에 걸린다. rownum=1 이 이걸 집어 드롭했다.
--
--   결과
--     ① 좁은 구버전 check 가 살아남아 REFUNDED / FAILED / EXPIRED 가
--        ORA-02290 으로 막힌다.
--     ② status / activity_type 의 NOT NULL 이 사라졌다.
--   no_data_found 를 삼키는 exception 블록 때문에 경고조차 남지 않았다.
--
-- 이 파일이 하는 일
--   1) 남아 있는 구버전 익명 check 를 대소문자 무관하게 찾아 드롭
--      (IS NOT NULL 조건식은 제외 — 같은 실수를 반복하지 않는다)
--   2) 사라진 NOT NULL 을 복구 (NULL 행이 있으면 중단하고 알린다)
--   3) 이름 있는 check 제약이 없으면 다시 만든다
--   4) admin_audit_log.target_type 의 열거 check 가 없으면 다시 만든다
--      (수동 정리 과정에서 익명 제약이 함께 지워질 수 있는 자리다)
--
--   전부 멱등이다. 이미 정상인 환경에서는 아무것도 바꾸지 않는다.
--
-- ⚠️ V1~V16 은 절대 수정하지 않는다 (체크섬 불일치 시 앱이 뜨지 않는다).
-- ==================================================================

-- ------------------------------------------------------------------
-- 1) 남아 있는 구버전 익명 check 드롭
-- ------------------------------------------------------------------
declare
    v_cnt pls_integer := 0;
begin
    for c in (
        select uc.table_name, uc.constraint_name
        from   user_constraints uc
        where  uc.constraint_type = 'C'
        and    uc.table_name in ('CHARGE_ORDER', 'ADMIN_AUDIT_LOG')
        and    uc.constraint_name like 'SYS\_C%' escape '\'
        -- NOT NULL 제약은 조건식이 "COL" IS NOT NULL 이다. 절대 건드리지 않는다.
        and    upper(nvl(uc.search_condition_vc, 'X')) not like '%IS NOT NULL%'
        -- 열거형 check 만 대상으로 한다 (대소문자 무관).
        and    upper(uc.search_condition_vc) like '%IN %(%'
    ) loop
        execute immediate 'alter table "' || c.table_name ||
                          '" drop constraint "' || c.constraint_name || '"';
        dbms_output.put_line('V17: dropped legacy check ' ||
                             c.table_name || '.' || c.constraint_name);
        v_cnt := v_cnt + 1;
    end loop;
    dbms_output.put_line('V17: legacy check dropped = ' || v_cnt);
end;
/

-- ------------------------------------------------------------------
-- 2) V6 / V7 이 잘못 지운 NOT NULL 복구
--
--    NULL 행이 남아 있으면 복구가 불가능하므로 명시적으로 중단한다.
--    (조용히 넘어가면 무결성 구멍이 그대로 남는다)
-- ------------------------------------------------------------------
declare
    procedure restore_not_null(p_table varchar2, p_column varchar2) is
        v_nullable user_tab_columns.nullable%type;
        v_nulls    number;
    begin
        select nullable into v_nullable
        from   user_tab_columns
        where  table_name = p_table and column_name = p_column;

        if v_nullable = 'N' then
            dbms_output.put_line('V17: ' || p_table || '.' || p_column ||
                                 ' already NOT NULL - skip');
            return;
        end if;

        execute immediate 'select count(*) from "' || p_table ||
                          '" where "' || p_column || '" is null'
        into v_nulls;

        if v_nulls > 0 then
            raise_application_error(-20017,
                p_table || '.' || p_column || ' 에 NULL 이 ' || v_nulls ||
                ' 건 있어 NOT NULL 을 복구할 수 없다. 해당 행을 먼저 정리할 것.');
        end if;

        execute immediate 'alter table "' || p_table ||
                          '" modify ("' || p_column || '" not null)';
        dbms_output.put_line('V17: restored NOT NULL on ' ||
                             p_table || '.' || p_column);
    end;
begin
    restore_not_null('CHARGE_ORDER',    'STATUS');
    restore_not_null('ADMIN_AUDIT_LOG', 'ACTIVITY_TYPE');
end;
/

-- ------------------------------------------------------------------
-- 3) 이름 있는 check 제약이 없으면 재생성
--
--    수동 복구 과정에서 함께 지워졌을 수 있다. 있으면 건드리지 않는다.
--    값 목록은 V7 / V8 의 최종 상태와 동일해야 한다.
-- ------------------------------------------------------------------
declare
    v_exists pls_integer;
begin
    select count(*) into v_exists
    from   user_constraints
    where  constraint_name = 'CK_CHARGE_ORDER_STATUS';

    if v_exists = 0 then
        execute immediate q'[
            alter table charge_order add constraint ck_charge_order_status
            check (status in ('READY','PAID','FAILED','REFUNDED','CANCELLED','EXPIRED'))]';
        dbms_output.put_line('V17: recreated ck_charge_order_status');
    end if;

    select count(*) into v_exists
    from   user_constraints
    where  constraint_name = 'CK_ADMIN_AUDIT_LOG_ACTIVITY';

    if v_exists = 0 then
        execute immediate q'[
            alter table admin_audit_log add constraint ck_admin_audit_log_activity
            check (activity_type in (
                'ADMIN_LOGIN','ADMIN_LOGOUT',
                'MEMBER_UPDATE','MEMBER_DELETE','MEMBER_ROLE_UPDATE','MEMBER_BAN',
                'POST_DELETE','WALLET_BALANCE_ADJUST','PAYMENT_REFUND','CHARGE_CANCEL',
                'INQUIRY_ANSWER_CREATE','INQUIRY_ANSWER_UPDATE','INQUIRY_ANSWER_DELETE',
                'NOTICE_CREATE','NOTICE_UPDATE','NOTICE_DELETE',
                'NOTIFICATION_BROADCAST','HARMFUL_KEYWORD_ADD','HARMFUL_KEYWORD_DELETE'))]';
        dbms_output.put_line('V17: recreated ck_admin_audit_log_activity');
    end if;
end;
/

-- ------------------------------------------------------------------
-- 4) admin_audit_log.target_type 열거 check 복구
--
--    V2 가 인라인으로 만든 익명 제약이라 이름이 SYS_Cxxxxxx 다.
--    "익명 제약 = 구버전 잔재" 로 보고 일괄 정리하다 보면 함께 지워지기 쉽다.
--    여기서 이름을 붙여 다시 만들어, 다음부터는 지목이 가능하게 한다.
--    값 목록은 V2 원문과 동일하다.
-- ------------------------------------------------------------------
declare
    v_exists pls_integer;
begin
    select count(*) into v_exists
    from   user_constraints
    where  table_name = 'ADMIN_AUDIT_LOG'
    and    constraint_type = 'C'
    and    upper(nvl(search_condition_vc, 'X')) not like '%IS NOT NULL%'
    and    upper(search_condition_vc) like '%TARGET_TYPE%';

    if v_exists = 0 then
        execute immediate q'[
            alter table admin_audit_log add constraint ck_admin_audit_log_target
            check (target_type in (
                'MEMBER','WALLET','POST','PAYMENT','INQUIRY',
                'NOTICE','NOTIFICATION','HARMFUL_KEYWORD'))]';
        dbms_output.put_line('V17: recreated target_type check as ck_admin_audit_log_target');
    else
        dbms_output.put_line('V17: target_type check present - skip');
    end if;
end;
/
