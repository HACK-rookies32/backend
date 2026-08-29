-- ==================================================================
-- V10 : device_token 을 "계정당 1행" -> "기기(FCM 토큰)당 1행" 으로.
--
-- 배경
--   부모가 폰+태블릿을 함께 쓰거나 기기 교체 직후 두 토큰이 잠깐 함께 유효한
--   구간이 있다. member_id 가 UNIQUE 면 나중 등록이 이전 기기의 토큰을 덮어써
--   그 기기는 푸시를 못 받는다. 또, 기기를 공유하며 계정만 바꿔 로그인하면
--   (member_id 로만 upsert 하던 탓에) 같은 토큰이 두 계정에 물려 이전 사용자
--   알림이 새 사용자 기기로 갔다.
--
--   -> token 을 유일 키로 두고, member_id 는 일반 인덱스로 바꾼다.
--      등록은 findByToken 으로 주인만 갱신한다(애플리케이션 코드).
--
-- 로컬/CI 는 H2 + ddl-auto 라 이 파일을 타지 않는다. 운영(Oracle 19)만 실행된다.
-- ⚠️ V1~V9 는 이미 적용 이력이 있으므로 수정하지 않는다. 적용 후 이 파일도 고치지 말 것.
-- ==================================================================

-- (1) 혹시 같은 토큰이 여러 계정에 물려 있으면(기기 공유 이력) 가장 최근 행만 남긴다.
--     아래 UNIQUE(token) 추가가 실패하지 않도록 먼저 정리한다.
DELETE FROM device_token d
 WHERE d.id < (SELECT MAX(d2.id) FROM device_token d2 WHERE d2.token = d.token);

-- (2) member_id 의 시스템 생성 UNIQUE 제약을 찾아서 드롭.
--     V1 이 인라인 `unique` 로 만들어 이름이 SYS_Cxxxx 다(V4 주석과 같은 사정).
DECLARE
  v_name user_constraints.constraint_name%TYPE;
BEGIN
  SELECT uc.constraint_name
    INTO v_name
    FROM user_constraints uc
    JOIN user_cons_columns ucc
      ON uc.constraint_name = ucc.constraint_name
   WHERE uc.table_name = 'DEVICE_TOKEN'
     AND uc.constraint_type = 'U'
     AND ucc.column_name = 'MEMBER_ID';
  EXECUTE IMMEDIATE 'ALTER TABLE device_token DROP CONSTRAINT ' || v_name;
EXCEPTION
  WHEN NO_DATA_FOUND THEN NULL;  -- 이미 없으면 그냥 통과
END;
/

-- (3) 같은 FCM 토큰은 한 행만.
ALTER TABLE device_token ADD CONSTRAINT uq_device_token_token UNIQUE (token);

-- (4) member_id 조회용 일반 인덱스(더 이상 unique 아님). findByMemberId / findAllByMemberIdIn 용.
CREATE INDEX idx_device_token_member ON device_token (member_id);
