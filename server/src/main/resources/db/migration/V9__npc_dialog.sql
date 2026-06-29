-- NPC dialog: free-text lines shown when a player walks up to an NPC and talks.
-- Lines are separated by '\n'. Faithful to the original walk-up-and-read behaviour.
ALTER TABLE npcs ADD COLUMN dialog TEXT;

-- Give the existing trainers a short taunt so talking to them reads naturally
-- before the duel is started.
UPDATE npcs SET dialog = E'Ngươi muốn thử sức với ta sao?\nHãy rút thú cưng ra và chiến đấu!'
WHERE npc_type = 'BATTLE_TRAINER';

-- A handful of plain DIALOG NPCs on the starter maps: guides and lore-givers.
-- Columns: name, sprite_id, npc_type, map_id, pos_x, pos_y, dialog_key, dialog
INSERT INTO npcs (name, sprite_id, npc_type, map_id, pos_x, pos_y, dialog_key, dialog) VALUES
    ('Già Làng', 3, 'DIALOG', 1, 4, 4, 'elder_intro',
     E'Chào mừng đến Vương Quốc Sủng Vật!\nHãy bắt và huấn luyện thú cưng để trở nên mạnh mẽ.\nĐi về phía đông để gặp huấn luyện viên đầu tiên.'),
    ('Cô Bán Hàng', 7, 'DIALOG', 1, 8, 3, 'shop_hint',
     E'Ghé cửa hàng để mua thuốc hồi máu nhé.\nThú cưng khoẻ mạnh mới thắng được trận khó.'),
    ('Nhà Thông Thái', 9, 'DIALOG', 2, 6, 5, 'lore_elements',
     E'Mỗi thú cưng mang một hệ: Mộc, Thổ, Thuỷ, Hoả, Ma, Phong, Điện.\nHệ khắc chế gây gấp ba sát thương — hãy chọn đối thủ khôn ngoan!');
