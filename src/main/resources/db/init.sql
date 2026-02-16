-- ============================================================
-- Dooring Partners - Affiliate Platform Schema (v0.2)
-- PostgreSQL 15+ / Supabase compatible
-- ============================================================
-- 변경 이력 (v0.1 → v0.2)
-- 1. campaigns: starts_at/ends_at 추가, 잘못된 UNIQUE 제약 제거 (partial index만 유지)
-- 2. products: store_id 추가, platform_id 제거 (store 통해 조인)
-- 3. order_items 테이블 추가 (주문-상품 연결)
-- 4. DDL 순서 수정 (attribution_sessions → pixel_events)
-- 5. clicks.link_created_at 제거
-- 6. sellers/creators에 auth 확장 고려한 구조 정리
-- ============================================================

-- ====================
-- ENUM TYPES
-- ====================

CREATE TYPE order_status_enum AS ENUM (
'CREATED',      -- 주문 생성
'PAID',         -- 결제 완료
'DELIVERED',    -- 배송 완료
'CONFIRMED',    -- 구매 확정
'CANCELLED',    -- 주문 취소
'REFUNDED'      -- 환불 완료
);

CREATE TYPE commission_status_enum AS ENUM (
'PENDING',      -- 커미션 대기 (구매 확정 전)
'CONFIRMED',    -- 커미션 확정 (구매 확정 후, 셀러 지급 의무 발생)
'PAID',         -- 셀러가 크리에이터에게 지급 완료
'CANCELLED'     -- 커미션 취소 (환불 등)
);

-- ====================
-- 1. PLATFORMS
-- ====================
-- 카페24, 아임웹 등 연동 쇼핑몰 플랫폼

CREATE TABLE platforms (
id              BIGSERIAL PRIMARY KEY,
code            TEXT NOT NULL UNIQUE,                -- 'cafe24', 'imweb' 등 코드값
name            TEXT NOT NULL,                       -- 표시명
base_api_url    TEXT,                                -- API base URL (연동용)
created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE platforms IS '연동 쇼핑몰 플랫폼 (카페24, 아임웹 등)';

-- ====================
-- 2. SELLERS
-- ====================
-- 쇼핑몰 사장님 (우리 SaaS 고객)

CREATE TABLE sellers (
id              BIGSERIAL PRIMARY KEY,
email           TEXT NOT NULL UNIQUE,                -- 로그인 식별자
name            TEXT NOT NULL,                       -- 사업자명 또는 대표자명
password_hash   TEXT,                                -- 자체 인증 시 사용
auth_provider   TEXT DEFAULT 'email',                -- 'email', 'cafe24_oauth', 'imweb_oauth' 등
created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE sellers IS '셀러 (쇼핑몰 운영자, SaaS 결제 주체)';

-- ====================
-- 3. STORES
-- ====================
-- 셀러가 플랫폼에 보유한 스토어 (셀러 1 : N 스토어)

CREATE TABLE stores (
id                  BIGSERIAL PRIMARY KEY,
seller_id           BIGINT NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
platform_id         BIGINT NOT NULL REFERENCES platforms(id) ON DELETE RESTRICT,
external_store_id   TEXT NOT NULL,                   -- 플랫폼에서 부여한 스토어 ID
name                TEXT,                            -- 스토어 표시명
access_token        TEXT,                            -- 플랫폼 API 토큰 (암호화 저장 권장)
is_active           BOOLEAN NOT NULL DEFAULT true,
created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT stores_platform_ext_uniq UNIQUE (platform_id, external_store_id)
);

COMMENT ON TABLE stores IS '셀러의 개별 스토어 (플랫폼당 고유)';

-- ====================
-- 4. CREATORS
-- ====================
-- 마케터/크리에이터 (오픈 가입)

CREATE TABLE creators (
id              BIGSERIAL PRIMARY KEY,
email           TEXT NOT NULL UNIQUE,                -- 로그인 식별자
nickname        TEXT NOT NULL UNIQUE,                -- 공개 닉네임
password_hash   TEXT,
auth_provider   TEXT DEFAULT 'email',
created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE creators IS '어필리에이트 크리에이터 (오픈 마켓 참여자)';

-- ====================
-- 5. PRODUCTS
-- ====================
-- 스토어에 속한 상품 (플랫폼 API 동기화 단위)
-- platform_id는 store → platform 조인으로 해결

CREATE TABLE products (
id                      BIGSERIAL PRIMARY KEY,
store_id                BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
external_product_id     TEXT NOT NULL,               -- 플랫폼 API가 제공하는 상품 ID
name                    TEXT,                        -- 상품명
image_url               TEXT,                        -- 대표 이미지 (큐레이션 페이지용)
product_url             TEXT,                        -- 상품 상세 페이지 URL (리다이렉트 대상)
price                   NUMERIC(18, 2),              -- 현재 판매가 (동기화)
is_active               BOOLEAN NOT NULL DEFAULT true,
last_synced_at          TIMESTAMPTZ,                 -- 마지막 API 동기화 시점
created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT products_store_ext_uniq UNIQUE (store_id, external_product_id)
);

COMMENT ON TABLE products IS '스토어 상품 (플랫폼 API 동기화 기준 단위)';

-- ====================
-- 6. CAMPAIGNS
-- ====================
-- 셀러가 상품에 대해 발행하는 커미션 캠페인
-- 한 상품에 동시에 활성 캠페인은 1개만 가능

CREATE TABLE campaigns (
id                      BIGSERIAL PRIMARY KEY,
product_id              BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
seller_id               BIGINT NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,

    -- 커미션 설정
    commission_amount       NUMERIC(18, 2) NOT NULL,     -- 건당 고정 커미션 금액 (KRW)
    commission_rate         NUMERIC(5, 4),               -- 참고용 비율 (e.g. 0.0300 = 3%)
    min_commission          NUMERIC(18, 2) NOT NULL DEFAULT 3000, -- 최소 커미션 (max(3%, 3000원) 정책)

    -- 캠페인 기간
    starts_at               TIMESTAMPTZ NOT NULL,
    ends_at                 TIMESTAMPTZ NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT true, -- 셀러 수동 중단용 플래그

    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- 기간 유효성: 종료 > 시작, 최소 3개월
    CONSTRAINT campaigns_valid_period CHECK (ends_at > starts_at),
    CONSTRAINT campaigns_min_duration CHECK (ends_at >= starts_at + INTERVAL '90 days')
);

-- 핵심: 활성 캠페인은 상품당 1개만
CREATE UNIQUE INDEX campaigns_product_active_uidx
ON campaigns (product_id)
WHERE is_active = true;

-- 기간 기반 조회 최적화
CREATE INDEX campaigns_period_idx ON campaigns (starts_at, ends_at);

COMMENT ON TABLE campaigns IS '커미션 캠페인 (상품당 활성 1개, 최소 3개월)';
COMMENT ON COLUMN campaigns.commission_amount IS '건당 고정 커미션 (캠페인 생성 시 확정)';
COMMENT ON COLUMN campaigns.is_active IS 'true=운영중, false=셀러 수동 중단. 기간 만료는 starts_at/ends_at로 판단';

-- ====================
-- 7. LINKS
-- ====================
-- 크리에이터가 상품에 대해 발급받는 어필리에이트 링크
-- 크리에이터 × 상품 = 1개 링크 (캠페인과 무관하게 유지)

CREATE TABLE links (
id              BIGSERIAL PRIMARY KEY,
creator_id      BIGINT NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
short_code      TEXT NOT NULL UNIQUE,                -- 리다이렉트용 고유 코드 (e.g. "aB3kX9")
created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT links_creator_product_uniq UNIQUE (creator_id, product_id)
);

CREATE INDEX links_short_code_idx ON links (short_code);

COMMENT ON TABLE links IS '어필리에이트 링크 (크리에이터 × 상품 = 1:1)';
COMMENT ON COLUMN links.short_code IS '리다이렉트 URL에 사용되는 고유 단축 코드';

-- ====================
-- 8. CLICKS
-- ====================
-- 링크 클릭 로그 + 클릭 시점 캠페인/커미션 스냅샷

CREATE TABLE clicks (
id                          BIGSERIAL PRIMARY KEY,
link_id                     BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,

    -- 클릭 시점 캠페인 스냅샷 (귀속 판단용)
    campaign_id                 BIGINT REFERENCES campaigns(id),
    commission_snapshot_amount  NUMERIC(18, 2),          -- 클릭 시점 커미션 금액
    commission_snapshot_rate    NUMERIC(5, 4),            -- 클릭 시점 커미션 비율

    -- 추적 메타
    click_token                 TEXT NOT NULL UNIQUE,     -- URL/쿠키/세션에 박히는 고유 토큰
    ip_address                  INET,                    -- IP (중복 클릭 필터링용)
    user_agent                  TEXT,

    clicked_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 어트리뷰션 윈도우 조회: link_id + 시간 범위
CREATE INDEX clicks_link_clicked_at_idx ON clicks (link_id, clicked_at);
-- click_token 기반 조회 (구매 귀속 시)
CREATE INDEX clicks_token_idx ON clicks (click_token);

COMMENT ON TABLE clicks IS '클릭 로그 (캠페인 스냅샷 포함, 어트리뷰션 윈도우 24h)';
COMMENT ON COLUMN clicks.campaign_id IS '클릭 시점에 활성이던 캠페인 (NULL이면 비활성 기간 클릭)';
COMMENT ON COLUMN clicks.click_token IS 'URL 파라미터 + 쿠키 + 세션 스토리지에 저장되는 추적 토큰';

-- ====================
-- 9. ORDERS
-- ====================
-- 플랫폼 API/웹훅으로 수신한 주문

CREATE TABLE orders (
id                  BIGSERIAL PRIMARY KEY,
store_id            BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
external_order_id   TEXT NOT NULL,                   -- 플랫폼 주문 ID
status              order_status_enum NOT NULL DEFAULT 'CREATED',
total_amount        NUMERIC(18, 2),                  -- 총 결제 금액
ordered_at          TIMESTAMPTZ,                     -- 실제 주문 시각 (플랫폼 제공)
created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- idempotent: 스토어 + 외부 주문 ID 조합으로 중복 방지
    CONSTRAINT orders_store_order_uniq UNIQUE (store_id, external_order_id)
);

COMMENT ON TABLE orders IS '주문 (플랫폼 웹훅/API 수신, idempotent)';

-- ====================
-- 10. ORDER ITEMS
-- ====================
-- 주문에 포함된 개별 상품 (어트리뷰션 시 "클릭한 상품이 실제 주문에 포함됐나" 검증용)

CREATE TABLE order_items (
id                      BIGSERIAL PRIMARY KEY,
order_id                BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
external_product_id     TEXT NOT NULL,               -- 플랫폼 상품 ID (products.external_product_id와 매칭)
product_name            TEXT,                        -- 상품명 스냅샷
quantity                INT NOT NULL DEFAULT 1,
item_amount             NUMERIC(18, 2),              -- 상품별 결제 금액
created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT order_items_order_product_uniq UNIQUE (order_id, external_product_id)
);

COMMENT ON TABLE order_items IS '주문 내 개별 상품 (구매 귀속 검증용)';

-- ====================
-- 11. ATTRIBUTION SESSIONS
-- ====================
-- click_token과 클릭을 연결하는 세션 (pixel_events보다 먼저 생성되어야 FK 참조 가능)

CREATE TABLE attribution_sessions (
id              BIGSERIAL PRIMARY KEY,
session_token   TEXT NOT NULL UNIQUE,                -- click_token과 동일하거나 파생된 세션 식별자
click_id        BIGINT NOT NULL REFERENCES clicks(id) ON DELETE CASCADE,
expires_at      TIMESTAMPTZ NOT NULL,                -- 어트리뷰션 윈도우 만료 (click + 24h)
created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX attribution_sessions_expires_idx ON attribution_sessions (expires_at);

COMMENT ON TABLE attribution_sessions IS '클릭-세션 매핑 (24h 어트리뷰션 윈도우)';

-- ====================
-- 12. PIXEL EVENTS
-- ====================
-- 클라이언트 사이드 픽셀로 수신한 전환 이벤트 (웹훅보다 먼저 도착할 수 있음)

CREATE TABLE pixel_events (
id                      BIGSERIAL PRIMARY KEY,
store_id                BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
external_order_id       TEXT NOT NULL,               -- 픽셀이 보고한 주문 ID
attribution_session_id  BIGINT REFERENCES attribution_sessions(id),
event_time              TIMESTAMPTZ NOT NULL DEFAULT now(),
created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- idempotent: 같은 스토어 + 주문 ID 픽셀 중복 방지
    CONSTRAINT pixel_store_order_uniq UNIQUE (store_id, external_order_id)
);

COMMENT ON TABLE pixel_events IS '픽셀 전환 이벤트 (idempotent, 웹훅 선/후행 모두 대응)';

-- ====================
-- 13. ATTRIBUTIONS
-- ====================
-- 주문 ↔ 클릭 귀속 (last-click 기준, 주문당 1건)

CREATE TABLE attributions (
id              BIGSERIAL PRIMARY KEY,
order_id        BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
click_id        BIGINT NOT NULL REFERENCES clicks(id) ON DELETE SET NULL,
campaign_id     BIGINT NOT NULL REFERENCES campaigns(id),   -- 귀속된 캠페인
attributed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- 주문당 귀속은 1건만
    CONSTRAINT attributions_order_uniq UNIQUE (order_id)
);

COMMENT ON TABLE attributions IS '구매 귀속 (last-click, 주문당 1건)';

-- ====================
-- 14. COMMISSION LEDGERS
-- ====================
-- 커미션 정산 원장

CREATE TABLE commission_ledgers (
id                  BIGSERIAL PRIMARY KEY,
attribution_id      BIGINT NOT NULL REFERENCES attributions(id) ON DELETE RESTRICT,
campaign_id         BIGINT NOT NULL REFERENCES campaigns(id),
creator_id          BIGINT NOT NULL REFERENCES creators(id) ON DELETE RESTRICT,
seller_id           BIGINT NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,

    amount              NUMERIC(18, 2) NOT NULL,         -- 커미션 금액
    status              commission_status_enum NOT NULL DEFAULT 'PENDING',

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at        TIMESTAMPTZ,                     -- 구매 확정 시점
    paid_at             TIMESTAMPTZ,                     -- 셀러→크리에이터 지급 시점

    -- 귀속당 커미션 1건
    CONSTRAINT commission_ledger_attr_uniq UNIQUE (attribution_id)
);

-- 셀러별 미지급 커미션 조회
CREATE INDEX commission_ledgers_seller_status_idx
ON commission_ledgers (seller_id, status);

-- 크리에이터별 커미션 조회
CREATE INDEX commission_ledgers_creator_status_idx
ON commission_ledgers (creator_id, status);

COMMENT ON TABLE commission_ledgers IS '커미션 정산 원장 (셀러→크리에이터 직접 지급)';

-- ====================
-- updated_at 자동 갱신 트리거
-- ====================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- updated_at 컬럼이 있는 모든 테이블에 트리거 적용
DO $$
DECLARE
t TEXT;
BEGIN
FOR t IN
SELECT table_name
FROM information_schema.columns
WHERE column_name = 'updated_at'
AND table_schema = 'public'
LOOP
EXECUTE format(
'CREATE TRIGGER trg_%s_updated_at
BEFORE UPDATE ON %I
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();',
t, t
);
END LOOP;
END;
$$;
