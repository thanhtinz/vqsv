-- V5__link_trainer_npcs.sql
-- Link each BATTLE_TRAINER npc to the enemy template it fights, so a duel can be
-- started by walking up to the NPC on the map (the original offline behaviour).

ALTER TABLE npcs ADD COLUMN enemy_template_id SMALLINT REFERENCES npc_enemy_templates(id);

-- The V4 seed gives each trainer NPC the same name as its enemy template.
UPDATE npcs n
SET enemy_template_id = t.id
FROM npc_enemy_templates t
WHERE n.npc_type = 'BATTLE_TRAINER' AND n.name = t.name;
