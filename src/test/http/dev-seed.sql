-- ============================================================
-- 로컬 개발용 더미데이터
-- 실행 전제: seller@test.com 회원가입 완료 (API로)
-- IntelliJ Database 패널 → dooring@localhost → Open Query Console
-- ============================================================

-- 0. 기존 더미데이터 초기화 (필요 시)
TRUNCATE campaigns, products, stores, platforms RESTART IDENTITY CASCADE;

-- 1. Platform
INSERT INTO platforms (code, name)
VALUES ('CUSTOM', '커스텀몰')
ON CONFLICT (code) DO NOTHING;

-- 2. Store (seller_id = 1 가정 — 실제 값으로 변경)
INSERT INTO stores (seller_id, platform_id, external_store_id, name)
VALUES (1, 1, 'test-store-001', '테스트 스토어')
ON CONFLICT (platform_id, external_store_id) DO NOTHING;

-- 3. Product
INSERT INTO products (store_id, external_product_id, name, product_url, price)
VALUES (1, 'prod-001', '테스트 상품', 'https://example.com/product/1', 29900)
ON CONFLICT (store_id, external_product_id) DO NOTHING;

-- 4. Campaign (최소 90일 필수)
INSERT INTO campaigns (product_id, seller_id, commission_amount, commission_rate, starts_at, ends_at)
VALUES (1, 1, 3000, 0.05, now(), now() + interval '91 days')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 확인 쿼리
-- ============================================================
SELECT 'platforms' AS tbl, count(*) FROM platforms
UNION ALL
SELECT 'stores',   count(*) FROM stores
UNION ALL
SELECT 'products', count(*) FROM products
UNION ALL
SELECT 'campaigns',count(*) FROM campaigns;
