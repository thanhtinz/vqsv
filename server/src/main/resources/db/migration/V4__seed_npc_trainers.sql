-- V4__seed_npc_trainers.sql
-- Seed NPC trainer duels — the original game's offline "đấu với NPC" battles,
-- remade for the online server. Each map gets a couple of trainer enemies whose
-- stats scale with the map's min level, plus BATTLE_TRAINER NPCs placed on maps.
-- A trainer battle is a normal PvE fight (not catchable) that grants the trainer's
-- fixed exp/gold reward on victory.

-- ============================================================
-- Trainer enemy templates (npc_enemy_templates)
-- ============================================================
-- Columns: name, sprite_id, level, hp, atk, def, spd, exp_reward, gold_reward, map_id
INSERT INTO npc_enemy_templates (name, sprite_id, level, hp, atk, def, spd, exp_reward, gold_reward, map_id) VALUES
    -- Làng Khởi Đầu (map 1) — beginner trainers
    ('Huấn Luyện Viên Tý',  1,  3,  70,  12,  6,  9,   40,   30, 1),
    ('Huấn Luyện Viên Sửu', 3,  5,  95,  15,  9,  7,   60,   45, 1),
    -- Rừng Xanh (map 2)
    ('Lữ Khách Dần',        5,  8, 140,  20, 11, 14,  110,   80, 2),
    ('Thợ Săn Mão',         7, 11, 190,  26, 16,  9,  170,  120, 2),
    -- Sa Mạc Lửa (map 3)
    ('Chiến Binh Thìn',     2, 16, 280,  36, 20, 16,  280,  200, 3),
    ('Pháp Sư Tỵ',          9, 19, 330,  44, 24, 19,  360,  260, 3),
    -- Đầm Lầy Tối (map 4)
    ('Sát Thủ Ngọ',        11, 26, 460,  58, 30, 26,  560,  400, 4),
    ('Tử Vệ Mùi',           8, 29, 540,  66, 38, 22,  700,  500, 4),
    -- Núi Băng (map 5)
    ('Băng Tướng Thân',     4, 36, 720,  84, 46, 30,  980,  700, 5),
    ('Hàn Vương Dậu',      10, 39, 820,  94, 52, 34, 1200,  860, 5),
    -- Tháp Bóng Tối (map 6) — boss-tier trainers
    ('Ma Tướng Tuất',      12, 46, 1100,118, 60, 40, 1700, 1200, 6),
    ('Hắc Đế Hợi',          6, 50, 1400,140, 72, 48, 2400, 1700, 6);

-- ============================================================
-- BATTLE_TRAINER NPCs placed on maps (npcs)
-- ============================================================
-- Columns: name, sprite_id, npc_type, map_id, pos_x, pos_y, dialog_key
INSERT INTO npcs (name, sprite_id, npc_type, map_id, pos_x, pos_y, dialog_key) VALUES
    ('Huấn Luyện Viên Tý',  1, 'BATTLE_TRAINER', 1,  6,  6, 'trainer_intro'),
    ('Lữ Khách Dần',        5, 'BATTLE_TRAINER', 2, 10, 12, 'trainer_intro'),
    ('Chiến Binh Thìn',     2, 'BATTLE_TRAINER', 3,  8,  9, 'trainer_intro'),
    ('Sát Thủ Ngọ',        11, 'BATTLE_TRAINER', 4, 11, 11, 'trainer_intro'),
    ('Băng Tướng Thân',     4, 'BATTLE_TRAINER', 5, 14, 10, 'trainer_intro'),
    ('Ma Tướng Tuất',      12, 'BATTLE_TRAINER', 6,  9, 18, 'trainer_intro');
