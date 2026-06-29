-- Map warps: stepping onto a warp tile teleports the player to another map.
-- A linear world progression matching the maps' rising level requirement:
-- 1 Làng Khởi Đầu (20x20) -> 2 Rừng Xanh (30x30) -> 3 Sa Mạc Lửa (25x25)
-- -> 4 Đầm Lầy Tối (25x25) -> 5 Núi Băng (30x30) -> 6 Tháp Bóng Tối (20x40).
-- Each connection is bidirectional; the destination sits just inside the next map.
INSERT INTO map_warps (from_map, from_x, from_y, to_map, to_x, to_y) VALUES
    -- 1 <-> 2
    (1, 19, 10, 2, 1, 10),
    (2, 0, 10, 1, 18, 10),
    -- 2 <-> 3
    (2, 29, 15, 3, 1, 12),
    (3, 0, 12, 2, 28, 15),
    -- 3 <-> 4
    (3, 24, 12, 4, 1, 12),
    (4, 0, 12, 3, 23, 12),
    -- 4 <-> 5
    (4, 24, 12, 5, 1, 15),
    (5, 0, 15, 4, 23, 12),
    -- 5 <-> 6
    (5, 29, 15, 6, 10, 1),
    (6, 10, 0, 5, 28, 15);
