-- ==================================================================
-- V25 : email_verification.purpose CHECK 제약에 SIGNUP_PHONE 추가 — 회원가입 휴대폰 인증.
--
-- 무엇을 하나
--   회원가입 휴대폰 인증(PhoneVerificationService)이 별도 테이블 없이
--   email_verification(purpose=SIGNUP_PHONE)을 재사용한다(TX_OTP 와 동일 방식,
--   email 컬럼=전화번호, 가입 전이라 target_member_id=0). 그런데 현재 열거 CHECK 제약은
--   V22 가 만든 3개(FIND_ID, RESET_PASSWORD, TX_OTP)뿐이라, SIGNUP_PHONE insert 시
--   ORA-02290(CHECK 위반) → 400 이 되어 인증코드 발송이 막힌다.
--
-- 이 파일이 하는 일 (전부 멱등)
--   1) purpose 에 걸린 구버전 익명 열거 check(SYS_Cxxxx)를 대소문자 무관하게 드롭
--      (NOT NULL 조건식은 제외 — V17/V22 와 동일 원칙)
--   2) V22 가 만든 이름 있는 3값 제약(ck_email_verification_purpose)을 드롭
--   3) SIGNUP_PHONE 포함 4값으로 이름 있는 제약 재생성
--
--   H2(로컬/CI)는 엔티티 enum(4개 전부 허용)에서 테이블을 재생성하므로 이 스크립트가
--   무의미하지만, Flyway 는 운영 Oracle 에서만 이 파일을 실행한다(V1~V24 와 동일 전제).
--
-- ⚠️ V1~V24 는 절대 수정하지 않는다 (체크섬 불일치 시 앱이 뜨지 않는다). 고칠 땐 이렇게 다음 번호로.
-- ==================================================================

-- ------------------------------------------------------------------
-- 1) purpose 에 걸린 구버전 익명 열거 check 드롭
-- ------------------------------------------------------------------
declare
    v_cnt pls_integer := 0;
begin
    for c in (
        select uc.constraint_name
        from   user_constraints uc
        where  uc.constraint_type = 'C'
        and    uc.table_name = 'EMAIL_VERIFICATION'
        and    uc.constraint_name like 'SYS\_C%' escape '\'
        and    upper(nvl(uc.search_condition_vc, 'X')) not like '%IS NOT NULL%'
        and    upper(uc.search_condition_vc) like '%PURPOSE%'
        and    upper(uc.search_condition_vc) like '%IN %(%'
    ) loop
        execute immediate 'alter table "EMAIL_VERIFICATION" drop constraint "' ||
                          c.constraint_name || '"';
        dbms_output.put_line('V25: dropped legacy purpose check ' || c.constraint_name);
        v_cnt := v_cnt + 1;
    end loop;
    dbms_output.put_line('V25: legacy purpose check dropped = ' || v_cnt);
end;
/

-- ------------------------------------------------------------------
-- 2) V22 가 만든 이름 있는 3값 제약을 드롭 (있을 때만)
-- ------------------------------------------------------------------
declare
    v_exists pls_integer;
begin
    select count(*) into v_exists
    from   user_constraints
    where  constraint_name = 'CK_EMAIL_VERIFICATION_PURPOSE';

    if v_exists > 0 then
        execute immediate 'alter table email_verification drop constraint ck_email_verification_purpose';
        dbms_output.put_line('V25: dropped ck_email_verification_purpose (3-value)');
    else
        dbms_output.put_line('V25: ck_email_verification_purpose absent - skip drop');
    end if;
end;
/

-- ------------------------------------------------------------------
-- 3) SIGNUP_PHONE 포함 4값으로 재생성
--
--    값 목록은 EmailVerification.Purpose enum 과 일치해야 한다.
-- ------------------------------------------------------------------
declare
    v_exists pls_integer;
begin
    select count(*) into v_exists
    from   user_constraints
    where  constraint_name = 'CK_EMAIL_VERIFICATION_PURPOSE';

    if v_exists = 0 then
        execute immediate q'[
            alter table email_verification add constraint ck_email_verification_purpose
            check (purpose in ('FIND_ID','RESET_PASSWORD','TX_OTP','SIGNUP_PHONE'))]';
        dbms_output.put_line('V25: recreated ck_email_verification_purpose (with SIGNUP_PHONE)');
    else
        dbms_output.put_line('V25: ck_email_verification_purpose present - skip create');
    end if;
end;
/
