-- V2: ip_address INET → TEXT
-- Hibernate 6 + PostgreSQL INET 타입 바인딩 불일치 해소
-- INET 유효성 검증은 애플리케이션 레이어에서 담당

ALTER TABLE clicks ALTER COLUMN ip_address TYPE TEXT;
