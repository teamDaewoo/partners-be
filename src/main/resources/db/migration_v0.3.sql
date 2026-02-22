-- ============================================================
-- Dooring Partners - Schema Migration v0.2 → v0.3
-- 변경 이력
-- 1. user_status_enum 타입 추가
-- 2. sellers: status, deleted_at 컬럼 추가
-- 3. creators: status, deleted_at 컬럼 추가
-- ============================================================

CREATE TYPE user_status_enum AS ENUM (
    'PENDING',      -- 가입 후 인증 전
    'ACTIVE',       -- 정상 활성
    'SUSPENDED'     -- 정지
);

ALTER TABLE sellers
    ADD COLUMN status     user_status_enum NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE creators
    ADD COLUMN status     user_status_enum NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN deleted_at TIMESTAMPTZ;
