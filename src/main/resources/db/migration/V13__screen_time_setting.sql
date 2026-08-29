-- ==================================================================
-- V13 : 자녀별 하루 총 스크린타임 한도.
--
-- 배경
--   앱 통제(app_control_rule)는 "앱별" 한도만 다룬다. "하루 전체 3시간" 같은 총량
--   개념이 서버에 없어서, 모바일이 180분을 상수로 들고 모든 자녀에게 같은 값을
--   보여 주고 있었다. 부모가 무엇을 설정하든 화면은 3시간이었다.
--
--   -> 자녀당 한 행으로 한도를 둔다. tracking_setting 과 같은 모양이다.
--
-- daily_limit_minutes 를 NULL 허용으로 둔 이유
--   NULL = 제한 없음, 0 = "한도 0분"(사실상 사용 금지) 으로 의미가 다르다.
--   둘을 구분해야 해서 NOT NULL + 센티넬 값 대신 nullable 로 둔다.
--
-- FK 를 걸지 않는 이유
--   기존 tracking_setting·notification 과 같은 사정이다. member 가 소프트 삭제되어도
--   설정 행이 남아 있어도 무방하고, 도메인 간 결합을 늘리지 않는다.
--
-- 로컬/CI 는 H2 + ddl-auto 라 이 파일을 타지 않는다. 운영(Oracle 19)만 실행된다.
-- ⚠️ V1~V12 는 이미 적용 이력이 있으므로 수정하지 않는다. 적용 후 이 파일도 고치지 말 것.
-- ==================================================================

CREATE TABLE screen_time_setting (
    id                  NUMBER(19) GENERATED AS IDENTITY PRIMARY KEY,
    child_id            NUMBER(19) NOT NULL,
    daily_limit_minutes NUMBER(10),
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT uq_screen_time_child UNIQUE (child_id)
);
