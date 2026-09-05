-- ==================================================================
-- V22 : email_verification.purpose CHECK 제약에 TX_OTP 추가 — 거래 OTP 발급 복구.
--
-- 무엇이 잘못됐나
--   거래 2차 인증(송금·회수·충전)은 별도 테이블 없이 email_verification(purpose=TX_OTP)을
--   재사용한다(EfinOtpService). 그런데 이 테이블의 purpose CHECK 제약은 V1 이 인라인으로 만든
--   익명 제약(SYS_Cxxxxxx)이라 값 목록이 최초 2개 그대로다:
--
--       purpose varchar2(255 char) not null check ((purpose in ('FIND_ID','RESET_PASSWORD')))
--
--   TX_OTP 를 insert 하면 ORA-02290(CHECK 위반) → DataIntegrityViolationException → 400
--   ("요청 값이 허용 범위를 벗어났습니다"). 그 결과 OTP 발급이 막혀 송금·회수·충전이 전부 실패한다.
--
--   로컬/CI(H2, ddl-auto)는 엔티티(enum 3개 전부 허용)에서 테이블을 재생성하므로 통과했고,
--   운영 Oracle(Flyway V1 제약)에서만 드러났다 — V19 가 "TX_OTP 재사용"을 명시했지만
--   CHECK 제약을 넓히는 마이그레이션을 만들지 않은 갭이다.
--
-- 이 파일이 하는 일
--   1) email_verification.purpose 에 걸린 구버전 익명 열거 check 를 대소문자 무관하게 찾아 드롭
--      (NOT NULL 조건식은 제외 — V17 과 같은 실수를 반복하지 않는다)
--   2) 이름 있는 열거 check(ck_email_verification_purpose)가 없으면 TX_OTP 포함해 재생성
--
--   전부 멱등이다. 이미 정상인 환경(넓은 제약이 이미 있는 경우)에서는 아무것도 바꾸지 않는다.
--   H2(로컬/CI)는 user_constraints 뷰가 없어 이 스크립트가 무의미하지만, Flyway 는 운영에서만
--   이 파일을 실행하므로 문제되지 않는다(V1~V21 과 동일 전제).
--
-- ⚠️ V1~V21 은 절대 수정하지 않는다 (체크섬 불일치 시 앱이 뜨지 않는다). 고칠 땐 이렇게 다음 번호로.
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
        -- NOT NULL 제약(조건식 "COL" IS NOT NULL)은 절대 건드리지 않는다.
        and    upper(nvl(uc.search_condition_vc, 'X')) not like '%IS NOT NULL%'
        -- purpose 열거 check 만 대상으로 한다 (대소문자 무관).
        and    upper(uc.search_condition_vc) like '%PURPOSE%'
        and    upper(uc.search_condition_vc) like '%IN %(%'
    ) loop
        execute immediate 'alter table "EMAIL_VERIFICATION" drop constraint "' ||
                          c.constraint_name || '"';
        dbms_output.put_line('V22: dropped legacy purpose check ' || c.constraint_name);
        v_cnt := v_cnt + 1;
    end loop;
    dbms_output.put_line('V22: legacy purpose check dropped = ' || v_cnt);
end;
/

-- ------------------------------------------------------------------
-- 2) TX_OTP 포함 열거 check 재생성 (이름 있는 제약이 없을 때만)
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
            check (purpose in ('FIND_ID','RESET_PASSWORD','TX_OTP'))]';
        dbms_output.put_line('V22: recreated ck_email_verification_purpose (with TX_OTP)');
    else
        dbms_output.put_line('V22: ck_email_verification_purpose present - skip');
    end if;
end;
/
