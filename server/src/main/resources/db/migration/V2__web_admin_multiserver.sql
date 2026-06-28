-- V2__web_admin_multiserver.sql
-- Multi-server architecture + website + admin platform tables.

-- ============================================================
-- GAME SERVERS REGISTRY (multi-server, cross-server, merge)
-- ============================================================
CREATE TABLE game_servers (
    id           SMALLSERIAL  PRIMARY KEY,
    code         VARCHAR(16)  NOT NULL UNIQUE,           -- s1, s2, ...
    name         VARCHAR(64)  NOT NULL,
    host         VARCHAR(128),
    tcp_port     INT          NOT NULL DEFAULT 9090,
    status       VARCHAR(16)  NOT NULL DEFAULT 'OPEN',   -- OPEN, MAINTENANCE, FULL, HIDDEN, MERGED
    cross_group  VARCHAR(32),                            -- lien-server group key (null = standalone)
    merged_into  SMALLINT     REFERENCES game_servers(id),
    sort_order   SMALLINT     NOT NULL DEFAULT 0,
    open_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
INSERT INTO game_servers (code, name, status, sort_order)
    VALUES ('s1', 'Máy Chủ 1: Khởi Nguyên', 'OPEN', 1);

-- ============================================================
-- ACCOUNTS: paid wallet + status (role column already exists)
-- ============================================================
ALTER TABLE accounts ADD COLUMN balance_xu  BIGINT      NOT NULL DEFAULT 0;   -- paid currency (xu)
ALTER TABLE accounts ADD COLUMN total_topup BIGINT      NOT NULL DEFAULT 0;   -- lifetime VND topped up
ALTER TABLE accounts ADD COLUMN status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'; -- ACTIVE, LOCKED

-- ============================================================
-- PLAYERS: server scope (name unique per-server instead of global)
-- ============================================================
ALTER TABLE players ADD COLUMN server_id SMALLINT NOT NULL DEFAULT 1 REFERENCES game_servers(id);
ALTER TABLE players DROP CONSTRAINT players_name_key;
ALTER TABLE players ADD CONSTRAINT players_server_name_uq UNIQUE (server_id, name);
CREATE INDEX idx_players_server ON players(server_id);

-- ============================================================
-- GIFTCODES
-- ============================================================
CREATE TABLE giftcodes (
    id           BIGSERIAL    PRIMARY KEY,
    code         VARCHAR(32)  NOT NULL UNIQUE,
    description  TEXT,
    reward_json  TEXT         NOT NULL DEFAULT '{}',     -- {"xu":100,"gold":1000,"items":[{"itemId":1,"qty":2}]}
    max_uses     INT          NOT NULL DEFAULT 0,        -- 0 = unlimited
    used_count   INT          NOT NULL DEFAULT 0,
    per_account  SMALLINT     NOT NULL DEFAULT 1,        -- redemptions allowed per account
    server_id    SMALLINT     REFERENCES game_servers(id), -- null = all servers
    starts_at    TIMESTAMPTZ,
    expires_at   TIMESTAMPTZ,
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DISABLED
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE TABLE giftcode_redemptions (
    id           BIGSERIAL    PRIMARY KEY,
    giftcode_id  BIGINT       NOT NULL REFERENCES giftcodes(id) ON DELETE CASCADE,
    account_id   BIGINT       NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    player_id    BIGINT       REFERENCES players(id) ON DELETE SET NULL,
    redeemed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_gcr_account ON giftcode_redemptions(account_id);
CREATE INDEX idx_gcr_giftcode ON giftcode_redemptions(giftcode_id);

-- ============================================================
-- TOP-UP PACKAGES + PAYMENT LEDGER
-- ============================================================
CREATE TABLE topup_packages (
    id           SMALLSERIAL  PRIMARY KEY,
    name         VARCHAR(64)  NOT NULL,
    price_vnd    INT          NOT NULL,
    xu_amount    INT          NOT NULL,                  -- base xu granted
    bonus_xu     INT          NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order   SMALLINT     NOT NULL DEFAULT 0
);
CREATE TABLE payment_transactions (
    id           BIGSERIAL    PRIMARY KEY,
    account_id   BIGINT       NOT NULL REFERENCES accounts(id),
    package_id   SMALLINT     REFERENCES topup_packages(id),
    amount_vnd   INT          NOT NULL,
    xu_granted   INT          NOT NULL DEFAULT 0,
    provider     VARCHAR(24)  NOT NULL DEFAULT 'MANUAL', -- MOMO, ZALOPAY, VNPAY, CARD, MANUAL
    provider_ref VARCHAR(128),
    status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING',-- PENDING, SUCCESS, FAILED, REFUNDED
    note         TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pt_account ON payment_transactions(account_id);
CREATE INDEX idx_pt_status  ON payment_transactions(status);

-- ============================================================
-- WEB SHOP (buy with xu, delivered to a character)
-- ============================================================
CREATE TABLE web_shop_products (
    id           SERIAL       PRIMARY KEY,
    name         VARCHAR(64)  NOT NULL,
    description  TEXT,
    icon_id      SMALLINT     NOT NULL DEFAULT 0,
    price_xu     INT          NOT NULL,
    reward_json  TEXT         NOT NULL DEFAULT '{}',
    stock        INT          NOT NULL DEFAULT -1,       -- -1 = unlimited
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order   SMALLINT     NOT NULL DEFAULT 0
);
CREATE TABLE web_shop_orders (
    id           BIGSERIAL    PRIMARY KEY,
    account_id   BIGINT       NOT NULL REFERENCES accounts(id),
    product_id   INT          NOT NULL REFERENCES web_shop_products(id),
    server_id    SMALLINT     NOT NULL REFERENCES game_servers(id),
    player_id    BIGINT       REFERENCES players(id),
    cost_xu      INT          NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'DELIVERED', -- DELIVERED, PENDING, FAILED
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_wso_account ON web_shop_orders(account_id);

-- ============================================================
-- NEWS & EVENTS
-- ============================================================
CREATE TABLE news_posts (
    id           BIGSERIAL    PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    slug         VARCHAR(220) NOT NULL UNIQUE,
    summary      TEXT,
    body         TEXT         NOT NULL,
    banner_url   VARCHAR(255),
    category     VARCHAR(24)  NOT NULL DEFAULT 'NEWS',   -- NEWS, UPDATE, GUIDE
    published    BOOLEAN      NOT NULL DEFAULT FALSE,
    author       VARCHAR(64),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);
CREATE TABLE events (
    id           BIGSERIAL    PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    body         TEXT         NOT NULL,
    banner_url   VARCHAR(255),
    starts_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ends_at      TIMESTAMPTZ,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================
-- AUDIT LOG (admin actions)
-- ============================================================
CREATE TABLE audit_logs (
    id           BIGSERIAL    PRIMARY KEY,
    actor_id     BIGINT       REFERENCES accounts(id),
    actor_name   VARCHAR(64),
    action       VARCHAR(64)  NOT NULL,
    target       VARCHAR(128),
    detail       TEXT,
    ip           VARCHAR(45),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_actor   ON audit_logs(actor_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- ============================================================
-- SEED: top-up packages, web shop products, sample news/event
-- ============================================================
INSERT INTO topup_packages (name, price_vnd, xu_amount, bonus_xu, sort_order) VALUES
    ('Gói 10K',  10000,  100,   0,    1),
    ('Gói 50K',  50000,  500,   50,   2),
    ('Gói 100K', 100000, 1000,  150,  3),
    ('Gói 200K', 200000, 2000,  400,  4),
    ('Gói 500K', 500000, 5000,  1500, 5);

INSERT INTO web_shop_products (name, description, icon_id, price_xu, reward_json, sort_order) VALUES
    ('Túi Tất Vàng x10', '10 Tất Vàng giao thẳng vào nhân vật', 3, 100,
        '{"items":[{"itemId":3,"qty":10}]}', 1),
    ('Gói Kim Tiền 50K', '50000 kim tiền', 9, 200,
        '{"gold":50000}', 2),
    ('Combo Tân Thủ', 'Tất + thuốc hồi + kim tiền cho người mới', 1, 50,
        '{"gold":10000,"items":[{"itemId":1,"qty":20},{"itemId":5,"qty":10}]}', 3);

INSERT INTO news_posts (title, slug, summary, body, category, published, author, published_at) VALUES
    ('Khai Mở Máy Chủ Khởi Nguyên',
     'khai-mo-may-chu-khoi-nguyen',
     'Vương Quốc Sủng Vật chính thức khai mở máy chủ đầu tiên!',
     'Chào mừng các bạn đến với Vương Quốc Sủng Vật Online. Máy chủ Khởi Nguyên đã chính thức khai mở. Đăng ký ngay để nhận quà tân thủ!',
     'NEWS', TRUE, 'Admin', NOW());

INSERT INTO events (title, body, active) VALUES
    ('Sự Kiện Khai Mở: Nạp Đầu x2',
     'Trong tuần khai mở, mọi giao dịch nạp lần đầu sẽ được nhân đôi số xu nhận được.',
     TRUE);
