-- V6__trainer_element.sql
-- Give NPC trainers/bosses an element so trainer duels respect the element chart
-- instead of always defaulting to FIRE.

ALTER TABLE npc_enemy_templates
    ADD COLUMN element VARCHAR(16) NOT NULL DEFAULT 'FIRE';

-- Spread the 12 seeded trainers (V4) across the 7 elements for variety.
UPDATE npc_enemy_templates SET element = CASE id
    WHEN 1  THEN 'WOOD'
    WHEN 2  THEN 'WATER'
    WHEN 3  THEN 'WIND'
    WHEN 4  THEN 'EARTH'
    WHEN 5  THEN 'FIRE'
    WHEN 6  THEN 'ELECTRIC'
    WHEN 7  THEN 'GHOST'
    WHEN 8  THEN 'WATER'
    WHEN 9  THEN 'WIND'
    WHEN 10 THEN 'ELECTRIC'
    WHEN 11 THEN 'GHOST'
    WHEN 12 THEN 'FIRE'
    ELSE element
END
WHERE id BETWEEN 1 AND 12;
