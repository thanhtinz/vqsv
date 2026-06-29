-- Quests: an NPC hands out an objective, the player completes it and returns to
-- claim the reward. Faithful to the original hunt / collect / level-up loop.
CREATE TABLE quests (
    id                    SMALLSERIAL PRIMARY KEY,
    name                  VARCHAR(64)  NOT NULL,
    giver_npc_id          SMALLINT     NOT NULL,
    description           TEXT,
    objective_type        VARCHAR(16)  NOT NULL DEFAULT 'KILL_MOB',  -- KILL_MOB | COLLECT_ITEM | REACH_LEVEL
    objective_target      SMALLINT     NOT NULL DEFAULT 0,
    objective_count       INTEGER      NOT NULL DEFAULT 1,
    reward_gold           INTEGER      NOT NULL DEFAULT 0,
    reward_exp            INTEGER      NOT NULL DEFAULT 0,
    reward_item_id        SMALLINT,
    required_level        SMALLINT     NOT NULL DEFAULT 1,
    prerequisite_quest_id SMALLINT
);

CREATE INDEX idx_quests_giver ON quests (giver_npc_id);

-- A player's progress on a quest.
CREATE TABLE player_quests (
    player_id  BIGINT      NOT NULL,
    quest_id   SMALLINT    NOT NULL,
    progress   INTEGER     NOT NULL DEFAULT 0,
    status     VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',  -- IN_PROGRESS | COMPLETED | CLAIMED
    PRIMARY KEY (player_id, quest_id)
);

CREATE INDEX idx_player_quests_player ON player_quests (player_id);

-- Starter quest chain. givers: 7=Già Làng, 8=Cô Bán Hàng, 9=Nhà Thông Thái.
-- targets: pet template ids (KILL_MOB) / item ids (COLLECT_ITEM) / level (REACH_LEVEL).
INSERT INTO quests (id, name, giver_npc_id, description, objective_type, objective_target,
                    objective_count, reward_gold, reward_exp, reward_item_id, required_level, prerequisite_quest_id) VALUES
    (1, 'Săn Nhiên Dực Bức', 7, E'Hãy hạ gục 3 Nhiên Dực Bức quanh làng để chứng tỏ bản lĩnh.',
        'KILL_MOB', 1, 3, 100, 50, NULL, 1, NULL),
    (2, 'Trưởng thành', 7, E'Luyện thú cưng đến cấp 5 rồi quay lại gặp ta.',
        'REACH_LEVEL', 5, 1, 200, 100, NULL, 1, 1),
    (3, 'Gom bẫy bắt', 8, E'Mang cho ta 5 Tất Thường, ta sẽ thưởng thuốc hồi máu.',
        'COLLECT_ITEM', 1, 5, 50, 0, 5, 1, NULL),
    (4, 'Thợ săn lão luyện', 9, E'Tiêu diệt 5 Nhiên Liệp Sư để học về sức mạnh các hệ.',
        'KILL_MOB', 3, 5, 300, 150, NULL, 3, NULL);

SELECT setval(pg_get_serial_sequence('quests', 'id'), (SELECT MAX(id) FROM quests));
