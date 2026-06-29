-- V8__trainer_party.sql
-- Trainers/bosses fight with a TEAM of enemies (the original summons the next enemy
-- when one faints). A BATTLE_TRAINER npc -> ordered list of npc_enemy_templates.

CREATE TABLE trainer_party (
    id                SERIAL    PRIMARY KEY,
    npc_id            SMALLINT  NOT NULL REFERENCES npcs(id) ON DELETE CASCADE,
    enemy_template_id SMALLINT  NOT NULL REFERENCES npc_enemy_templates(id),
    slot              SMALLINT  NOT NULL DEFAULT 0
);

CREATE INDEX idx_trainer_party_npc ON trainer_party(npc_id, slot);

-- Each BATTLE_TRAINER fights every enemy template on its map (2 per map), ordered by id.
INSERT INTO trainer_party (npc_id, enemy_template_id, slot)
SELECT n.id,
       t.id,
       (ROW_NUMBER() OVER (PARTITION BY n.id ORDER BY t.id) - 1)::smallint
FROM npcs n
JOIN npc_enemy_templates t ON t.map_id = n.map_id
WHERE n.npc_type = 'BATTLE_TRAINER';
