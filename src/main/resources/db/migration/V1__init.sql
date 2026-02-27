-- ============================================================
-- V1: 초기 스키마 (v0.2 + v0.3 통합)
-- ============================================================

-- ============================================================
-- ENUM Types
-- ============================================================

CREATE TYPE user_status_enum AS ENUM (
    'PENDING',      -- 가입 후 인증 전
    'ACTIVE',       -- 정상 활성
    'SUSPENDED'     -- 정지
);

CREATE TYPE order_status_enum AS ENUM (
    'CREATED',
    'PAID',
    'DELIVERED',
    'CONFIRMED',
    'CANCELLED',
    'REFUNDED'
);

CREATE TYPE commission_status_enum AS ENUM (
    'PENDING',
    'CONFIRMED',
    'PAID',
    'CANCELLED'
);

-- ============================================================
-- identity
-- ============================================================

CREATE TABLE platforms (
    id           BIGSERIAL PRIMARY KEY,
    code         TEXT NOT NULL UNIQUE,
    name         TEXT NOT NULL,
    base_api_url TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sellers (
    id            BIGSERIAL PRIMARY KEY,
    email         TEXT NOT NULL UNIQUE,
    name          TEXT NOT NULL,
    password_hash TEXT,
    auth_provider TEXT NOT NULL DEFAULT 'email',
    status        user_status_enum NOT NULL DEFAULT 'ACTIVE',
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE creators (
    id            BIGSERIAL PRIMARY KEY,
    email         TEXT NOT NULL UNIQUE,
    nickname      TEXT NOT NULL UNIQUE,
    password_hash TEXT,
    auth_provider TEXT NOT NULL DEFAULT 'email',
    status        user_status_enum NOT NULL DEFAULT 'ACTIVE',
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- catalog
-- ============================================================

CREATE TABLE stores (
    id                BIGSERIAL PRIMARY KEY,
    seller_id         BIGINT NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
    platform_id       BIGINT NOT NULL REFERENCES platforms(id) ON DELETE RESTRICT,
    external_store_id TEXT NOT NULL,
    name              TEXT,
    access_token      TEXT,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT stores_platform_ext_uniq UNIQUE (platform_id, external_store_id)
);

CREATE TABLE products (
    id                  BIGSERIAL PRIMARY KEY,
    store_id            BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    external_product_id TEXT NOT NULL,
    name                TEXT,
    image_url           TEXT,
    product_url         TEXT,
    price               NUMERIC(18, 2),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    last_synced_at      TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT products_store_ext_uniq UNIQUE (store_id, external_product_id)
);

CREATE TABLE campaigns (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    seller_id         BIGINT NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
    commission_amount NUMERIC(18, 2) NOT NULL,
    commission_rate   NUMERIC(5, 4),
    min_commission    NUMERIC(18, 2) NOT NULL DEFAULT 3000,
    starts_at         TIMESTAMPTZ NOT NULL,
    ends_at           TIMESTAMPTZ NOT NULL,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT campaigns_valid_period CHECK (ends_at > starts_at),
    CONSTRAINT campaigns_min_duration CHECK (ends_at >= starts_at + INTERVAL '90 days')
);

-- 활성 캠페인은 상품당 1개만 (partial index)
CREATE UNIQUE INDEX campaigns_product_active_uidx ON campaigns (product_id) WHERE is_active = TRUE;
CREATE INDEX campaigns_period_idx ON campaigns (starts_at, ends_at);

-- ============================================================
-- tracking
-- ============================================================

CREATE TABLE links (
    id         BIGSERIAL PRIMARY KEY,
    creator_id BIGINT NOT NULL REFERENCES creators(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    short_code TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT links_creator_product_uniq UNIQUE (creator_id, product_id)
);

CREATE INDEX links_short_code_idx ON links (short_code);

CREATE TABLE clicks (
    id                         BIGSERIAL PRIMARY KEY,
    link_id                    BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    campaign_id                BIGINT REFERENCES campaigns(id),
    commission_snapshot_amount NUMERIC(18, 2),
    commission_snapshot_rate   NUMERIC(5, 4),
    click_token                TEXT NOT NULL UNIQUE,
    ip_address                 INET,
    user_agent                 TEXT,
    clicked_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX clicks_link_clicked_at_idx ON clicks (link_id, clicked_at);
CREATE INDEX clicks_token_idx ON clicks (click_token);

CREATE TABLE attribution_sessions (
    id            BIGSERIAL PRIMARY KEY,
    session_token TEXT NOT NULL UNIQUE,
    click_id      BIGINT NOT NULL REFERENCES clicks(id) ON DELETE CASCADE,
    expires_at    TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX attribution_sessions_expires_idx ON attribution_sessions (expires_at);

CREATE TABLE pixel_events (
    id                     BIGSERIAL PRIMARY KEY,
    store_id               BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    external_order_id      TEXT NOT NULL,
    attribution_session_id BIGINT REFERENCES attribution_sessions(id),
    event_time             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pixel_store_order_uniq UNIQUE (store_id, external_order_id)
);

-- ============================================================
-- order
-- ============================================================

CREATE TABLE orders (
    id                BIGSERIAL PRIMARY KEY,
    store_id          BIGINT NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
    external_order_id TEXT NOT NULL,
    status            order_status_enum NOT NULL DEFAULT 'CREATED',
    total_amount      NUMERIC(18, 2),
    ordered_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT orders_store_order_uniq UNIQUE (store_id, external_order_id)
);

CREATE TABLE order_items (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    external_product_id TEXT NOT NULL,
    product_name        TEXT,
    quantity            INTEGER NOT NULL DEFAULT 1,
    item_amount         NUMERIC(18, 2),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT order_items_order_product_uniq UNIQUE (order_id, external_product_id)
);

-- ============================================================
-- attribution
-- ============================================================

CREATE TABLE attributions (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    click_id      BIGINT NOT NULL REFERENCES clicks(id) ON DELETE RESTRICT,
    campaign_id   BIGINT NOT NULL REFERENCES campaigns(id),
    attributed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT attributions_order_uniq UNIQUE (order_id)
);

CREATE INDEX attributions_order_id_idx ON attributions (order_id);

CREATE TABLE commission_ledgers (
    id             BIGSERIAL PRIMARY KEY,
    attribution_id BIGINT NOT NULL REFERENCES attributions(id) ON DELETE RESTRICT,
    campaign_id    BIGINT NOT NULL REFERENCES campaigns(id),
    creator_id     BIGINT NOT NULL REFERENCES creators(id) ON DELETE RESTRICT,
    seller_id      BIGINT NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
    amount         NUMERIC(18, 2) NOT NULL,
    status         commission_status_enum NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at   TIMESTAMPTZ,
    paid_at        TIMESTAMPTZ,
    CONSTRAINT commission_ledger_attr_uniq UNIQUE (attribution_id)
);

CREATE INDEX commission_ledgers_seller_status_idx ON commission_ledgers (seller_id, status);
CREATE INDEX commission_ledgers_creator_status_idx ON commission_ledgers (creator_id, status);

-- ============================================================
-- updated_at 자동 갱신 트리거
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

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
             FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();',
            t, t
        );
    END LOOP;
END;
$$;
