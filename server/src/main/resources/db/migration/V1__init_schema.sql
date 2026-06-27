-- V1__init_schema.sql
-- Vuong Quoc Sung Vat - Full Database Schema

CREATE TABLE accounts (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(32)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,
    email       VARCHAR(128) UNIQUE,
    phone       VARCHAR(20),
    role        VARCHAR(16)  NOT NULL DEFAULT 'PLAYER',
    is_banned   BOOLEAN      NOT NULL DEFAULT FALSE,
    ban_reason  TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_login  TIMESTAMPTZ
);

CREATE TABLE players (
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT       NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    name        VARCHAR(32)  NOT NULL UNIQUE,
    level       SMALLINT     NOT NULL DEFAULT 1,
    exp         INT          NOT NULL DEFAULT 0,
    kim_tien    INT          NOT NULL DEFAULT 0,
    huy_chuong  INT          NOT NULL DEFAULT 0,
    map_id      SMALLINT     NOT NULL DEFAULT 1,
    pos_x       SMALLINT     NOT NULL DEFAULT 5,
    pos_y       SMALLINT     NOT NULL DEFAULT 5,
    hp          INT          NOT NULL DEFAULT 100,
    hp_max      INT          NOT NULL DEFAULT 100,
    is_online   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE pet_templates (
    id          SMALLSERIAL  PRIMARY KEY,
    name        VARCHAR(32)  NOT NULL,
    sprite_id   SMALLINT     NOT NULL,
    element     VARCHAR(16)  NOT NULL,
    base_hp     SMALLINT     NOT NULL DEFAULT 50,
    base_atk    SMALLINT     NOT NULL DEFAULT 10,
    base_def    SMALLINT     NOT NULL DEFAULT 5,
    base_spd    SMALLINT     NOT NULL DEFAULT 5,
    growth_hp   FLOAT        NOT NULL DEFAULT 1.2,
    growth_atk  FLOAT        NOT NULL DEFAULT 1.15,
    growth_def  FLOAT        NOT NULL DEFAULT 1.1,
    growth_spd  FLOAT        NOT NULL DEFAULT 1.05,
    catch_rate  SMALLINT     NOT NULL DEFAULT 30,
    evolve_into SMALLINT     REFERENCES pet_templates(id),
    evolve_lv   SMALLINT,
    description TEXT
);

CREATE TABLE player_pets (
    id          BIGSERIAL    PRIMARY KEY,
    player_id   BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    template_id SMALLINT     NOT NULL REFERENCES pet_templates(id),
    nickname    VARCHAR(32),
    level       SMALLINT     NOT NULL DEFAULT 1,
    exp         INT          NOT NULL DEFAULT 0,
    hp          INT          NOT NULL DEFAULT 50,
    hp_max      INT          NOT NULL DEFAULT 50,
    atk         SMALLINT     NOT NULL DEFAULT 10,
    def         SMALLINT     NOT NULL DEFAULT 5,
    spd         SMALLINT     NOT NULL DEFAULT 5,
    slot        SMALLINT     NOT NULL DEFAULT 0,
    loyalty     SMALLINT     NOT NULL DEFAULT 50,
    obtained_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE maps (
    id          SMALLSERIAL  PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    width       SMALLINT     NOT NULL DEFAULT 20,
    height      SMALLINT     NOT NULL DEFAULT 20,
    tileset_id  SMALLINT     NOT NULL DEFAULT 0,
    bgm_id      SMALLINT     NOT NULL DEFAULT 0,
    is_pvp      BOOLEAN      NOT NULL DEFAULT FALSE,
    min_level   SMALLINT     NOT NULL DEFAULT 1
);

CREATE TABLE map_warps (
    id          SERIAL       PRIMARY KEY,
    from_map    SMALLINT     NOT NULL REFERENCES maps(id),
    from_x      SMALLINT     NOT NULL,
    from_y      SMALLINT     NOT NULL,
    to_map      SMALLINT     NOT NULL REFERENCES maps(id),
    to_x        SMALLINT     NOT NULL,
    to_y        SMALLINT     NOT NULL
);

CREATE TABLE map_wild_pets (
    id          SERIAL       PRIMARY KEY,
    map_id      SMALLINT     NOT NULL REFERENCES maps(id),
    template_id SMALLINT     NOT NULL REFERENCES pet_templates(id),
    min_level   SMALLINT     NOT NULL DEFAULT 1,
    max_level   SMALLINT     NOT NULL DEFAULT 5,
    spawn_rate  SMALLINT     NOT NULL DEFAULT 10
);

CREATE TABLE npcs (
    id          SMALLSERIAL  PRIMARY KEY,
    name        VARCHAR(32)  NOT NULL,
    sprite_id   SMALLINT     NOT NULL,
    npc_type    VARCHAR(16)  NOT NULL DEFAULT 'DIALOG',
    map_id      SMALLINT     NOT NULL REFERENCES maps(id),
    pos_x       SMALLINT     NOT NULL,
    pos_y       SMALLINT     NOT NULL,
    dialog_key  VARCHAR(64)
);

CREATE TABLE npc_enemy_templates (
    id          SMALLSERIAL  PRIMARY KEY,
    name        VARCHAR(32)  NOT NULL,
    sprite_id   SMALLINT     NOT NULL,
    level       SMALLINT     NOT NULL DEFAULT 1,
    hp          INT          NOT NULL DEFAULT 50,
    atk         SMALLINT     NOT NULL DEFAULT 8,
    def         SMALLINT     NOT NULL DEFAULT 3,
    spd         SMALLINT     NOT NULL DEFAULT 5,
    exp_reward  INT          NOT NULL DEFAULT 20,
    gold_reward INT          NOT NULL DEFAULT 10,
    map_id      SMALLINT     REFERENCES maps(id)
);

CREATE TABLE items (
    id          SMALLSERIAL  PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    item_type   VARCHAR(16)  NOT NULL,
    effect_val  INT          NOT NULL DEFAULT 0,
    icon_id     SMALLINT     NOT NULL DEFAULT 0,
    description TEXT
);

CREATE TABLE shop_listings (
    id          SERIAL       PRIMARY KEY,
    item_id     SMALLINT     NOT NULL REFERENCES items(id),
    price_gold  INT,
    price_medal INT,
    sort_order  SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE player_items (
    id          BIGSERIAL    PRIMARY KEY,
    player_id   BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    item_id     SMALLINT     NOT NULL REFERENCES items(id),
    quantity    INT          NOT NULL DEFAULT 1,
    UNIQUE(player_id, item_id)
);

CREATE TABLE badges (
    id          SMALLSERIAL  PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    icon_id     SMALLINT     NOT NULL,
    condition   VARCHAR(128) NOT NULL,
    description TEXT
);

CREATE TABLE player_badges (
    player_id   BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    badge_id    SMALLINT     NOT NULL REFERENCES badges(id),
    earned_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (player_id, badge_id)
);

CREATE TABLE battle_logs (
    id          BIGSERIAL    PRIMARY KEY,
    attacker_id BIGINT       NOT NULL REFERENCES players(id),
    defender_id BIGINT,
    winner_id   BIGINT,
    battle_type VARCHAR(16)  NOT NULL DEFAULT 'PVE',
    turns       SMALLINT     NOT NULL DEFAULT 0,
    exp_gained  INT          NOT NULL DEFAULT 0,
    gold_gained INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE sessions (
    id          VARCHAR(64)  PRIMARY KEY,
    player_id   BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    ip          VARCHAR(45),
    client_type VARCHAR(16)  NOT NULL DEFAULT 'WEB',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_players_account ON players(account_id);
CREATE INDEX idx_players_map ON players(map_id);
CREATE INDEX idx_player_pets_player ON player_pets(player_id);
CREATE INDEX idx_player_items_player ON player_items(player_id);
CREATE INDEX idx_map_wild_pets_map ON map_wild_pets(map_id);
CREATE INDEX idx_npcs_map ON npcs(map_id);
CREATE INDEX idx_battle_logs_attacker ON battle_logs(attacker_id);
CREATE INDEX idx_sessions_player ON sessions(player_id);

INSERT INTO maps (name, width, height, tileset_id, bgm_id, min_level) VALUES
    ('Làng Khởi Đầu', 20, 20, 0, 0, 1),
    ('Rừng Xanh', 30, 30, 1, 1, 5),
    ('Sa Mạc Lửa', 25, 25, 2, 2, 15),
    ('Đầm Lầy Tối', 25, 25, 3, 3, 25),
    ('Núi Băng', 30, 30, 4, 4, 35),
    ('Tháp Bóng Tối', 20, 40, 5, 5, 50);

INSERT INTO pet_templates (name, sprite_id, element, base_hp, base_atk, base_def, base_spd, catch_rate, evolve_into, evolve_lv) VALUES
    ('Thỏ Lửa',     1, 'FIRE',  60, 12, 5, 8,  40, 2, 20),
    ('Thỏ Lửa+',    2, 'FIRE',  90, 18, 8, 12, 15, NULL, NULL),
    ('Rùa Nước',    3, 'WATER', 80, 8,  12, 5,  40, 4, 20),
    ('Rùa Nước+',   4, 'WATER', 120, 12, 18, 7, 15, NULL, NULL),
    ('Chim Gió',    5, 'WIND',  50, 10, 4,  15, 40, 6, 20),
    ('Đại Bàng',    6, 'WIND',  70, 15, 6,  22, 15, NULL, NULL),
    ('Địa Long',    7, 'EARTH', 100,10, 10, 6,  35, 8, 20),
    ('Thần Long',   8, 'EARTH', 150,15, 15, 8,  10, NULL, NULL),
    ('Cáo Sáng',    9, 'LIGHT', 70, 11, 7,  10, 30, 10, 25),
    ('Cáo Thần',   10, 'LIGHT', 110,17, 10, 14,  8, NULL, NULL),
    ('Mèo Bóng',   11, 'DARK',  65, 14, 6,  12, 30, 12, 25),
    ('Quỷ Mèo',    12, 'DARK', 100, 20, 9,  18,  8, NULL, NULL);

INSERT INTO items (name, item_type, effect_val, icon_id, description) VALUES
    ('Tất Thường',     'CATCH_BALL', 30, 1, 'Bẫy bắt sủng vật cơ bản'),
    ('Tất Bạc',        'CATCH_BALL', 50, 2, 'Tăng tỷ lệ bắt sủng vật'),
    ('Tất Vàng',       'CATCH_BALL', 80, 3, 'Tỷ lệ bắt rất cao'),
    ('Tất Trúng Cầu',  'CATCH_BALL',100, 4, 'Bắt chắc chắn 100%'),
    ('Thuốc Hồi HP',   'MEDICINE',  50, 5, 'Hồi phục 50 HP cho sủng vật'),
    ('Thuốc Hồi Lớn',  'MEDICINE', 150, 6, 'Hồi phục 150 HP cho sủng vật'),
    ('Thuốc Hồi Đầy',  'MEDICINE',9999, 7, 'Hồi phục toàn bộ HP'),
    ('Kẹo Tăng Cấp',   'LEVEL_UP',   5, 8, 'Tăng 5 cấp cho sủng vật'),
    ('Gói Kim Tiền S',  'GOLD_PACK',1000, 9, '1000 kim tiền'),
    ('Gói Kim Tiền M',  'GOLD_PACK',5000,10, '5000 kim tiền'),
    ('Gói Kim Tiền L',  'GOLD_PACK',15000,11,'15000 kim tiền');

INSERT INTO shop_listings (item_id, price_gold, price_medal, sort_order) VALUES
    (1, 100,  NULL, 1),
    (2, 500,  NULL, 2),
    (3, 1500, NULL, 3),
    (4, NULL, 5,    4),
    (5, 200,  NULL, 5),
    (6, 600,  NULL, 6),
    (7, 2000, NULL, 7),
    (8, NULL, 3,    8);

INSERT INTO badges (name, icon_id, condition, description) VALUES
    ('Người Mới',     1, 'ACCOUNT_CREATED',  'Tạo tài khoản lần đầu'),
    ('Thợ Săn Mới',   2, 'CATCH_PETS:1',     'Bắt được sủng vật đầu tiên'),
    ('Thợ Săn Pro',   3, 'CATCH_PETS:50',    'Bắt được 50 sủng vật'),
    ('Chiến Binh',    4, 'WIN_BATTLES:10',   'Thắng 10 trận đấu'),
    ('Dũng Sĩ',       5, 'WIN_BATTLES:100',  'Thắng 100 trận đấu'),
    ('Nhà Giàu',      6, 'GOLD:10000',       'Tích lũy 10000 kim tiền'),
    ('Vô Địch',       7, 'PLAYER_LEVEL:50',  'Đạt cấp độ 50'),
    ('Huyền Thoại',   8, 'PLAYER_LEVEL:100', 'Đạt cấp độ tối đa');

INSERT INTO map_wild_pets (map_id, template_id, min_level, max_level, spawn_rate) VALUES
    (1, 1,  1,  3,  20), (1, 3,  1,  3,  20), (1, 5,  1,  3,  15),
    (2, 1,  5,  10, 15), (2, 7,  5,  10, 10), (2, 9,  5,  10, 8),
    (3, 2,  15, 25, 10), (3, 7,  15, 25, 10), (3, 11, 15, 25, 8),
    (4, 4,  25, 35, 8),  (4, 11, 25, 35, 8),
    (5, 6,  35, 50, 8),  (5, 8,  35, 50, 5),
    (6, 10, 50, 80, 5),  (6, 12, 50, 80, 5);
