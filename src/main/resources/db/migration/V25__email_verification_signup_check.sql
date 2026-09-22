-- ==================================================================
-- V25 : email_verification.purpose CHECK 제약에 SIGNUP 추가 — 회원가입 이메일 인증 발급 허용.
--
-- 무엇이 필요한가
--   회원가입 이메일 인증(SignupEmailVerificationService)은 별도 테이블 없이
--   email_verification(purpose=SIGNUP)을 재사용한다(TX_OTP 선례와 동일).
--   그런데 V22 가 만든 명명 제약 ck_email_verification_purpose 의 값 목록은
--
--       check (purpose in ('FIND_ID','RESET_PASSWORD','TX_OTP'))
--
--   이라 SIGNUP 을 insert 하면 ORA-02290(CHECK 위반) → DataIntegrityViolationException → 400.
--   그 결과 가입 이메일 인증코드 발송이 막힌다.
--
--   로컬/CI(H2, ddl-auto)는 엔티티(enum 4개 전부 허용)에서 테이블을 재생성하므로 통과하고,
--   운영 Oracle(V22 제약)에서만 드러난다.
--
-- 이 파일이 하는 일
--   1) V22 가 만든 명명 제약 ck_email_verification_purpose 가 있으면 드롭
--   2) 혹시 남아 있을 구버전 익명 열거 check(SYS_Cxxxxxx)도 대소문자 무관하게 드롭
--      (NOT NULL 조건식은 제외 — V17/V22 와 같은 실수를 반복하지 않는다)
--   3) SIGNUP 포함 열거 check 를 재생성
--
--   전부 멱등이다. H2(로컬/CI)는 user_constraints 뷰가 없어 무의미하지만, Flyway 는 운영에서만
--   이 파일을 실행하므로 문제되지 않는다(V1~V24 와 동일 전제).
--
-- ⚠️ V1~V24 는 절대 수정하지 않는다 (체크섬 불일치 시 앱이 뜨지 않는다). 고칠 땐 이렇게 다음 번호로.
-- ==================================================================

-- ------------------------------------------------------------------
-- 1) V22 명명 제약 드롭 (있을 때만)
-- ------------------------------------------------------------------
declare
    v_exists pls_integer;
begin
    select count(*) into v_exists
    from   user_constraints
    where  constraint_name = 'CK_EMAIL_VERIFICATION_PURPOSE';

    if v_exists > 0 then
        execute immediate 'alter table email_verification drop constraint ck_email_verification_purpose';
        dbms_output.put_line('V25: dropped ck_email_verification_purpose (V22)');
    else
        dbms_output.put_line('V25: ck_email_verification_purpose absent - skip drop');
    end if;
end;
/

-- ------------------------------------------------------------------
-- 2) 남아 있을 구버전 익명 열거 check 드롭 (안전망)
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
-- 3) SIGNUP 포함 열거 check 재생성 (이름 있는 제약이 없을 때만)
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
            check (purpose in ('FIND_ID','RESET_PASSWORD','TX_OTP','SIGNUP'))]';
        dbms_output.put_line('V25: recreated ck_email_verification_purpose (with SIGNUP)');
    else
        dbms_output.put_line('V25: ck_email_verification_purpose present - skip');
    end if;
end;
/
