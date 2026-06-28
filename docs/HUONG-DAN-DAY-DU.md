# VQSV — Hướng dẫn đầy đủ (A → Z)

Tài liệu thực hành: cài công cụ, chạy local, cấu hình, đấu domain, build client,
và **thêm map / NPC / mob / item / tính năng**. Mọi lệnh và đường dẫn ở đây là
thật trong repo này.

Mục lục:
1. [Công cụ cần cài](#1-công-cụ-cần-cài)
2. [Lấy mã nguồn & chạy thử local](#2-lấy-mã-nguồn--chạy-thử-local)
3. [Cấu hình (config) mọi thành phần](#3-cấu-hình-config-mọi-thành-phần)
4. [Đấu domain & deploy production](#4-đấu-domain--deploy-production)
5. [Build client lên từng nền tảng](#5-build-client-lên-từng-nền-tảng)
6. [Thêm nội dung game (map/mob/npc/item/...)](#6-thêm-nội-dung-game)
7. [Thêm tính năng mới (ví dụ thật)](#7-thêm-tính-năng-mới)
8. [Sprite ID & asset](#8-sprite-id--asset)
9. [Lệnh hữu ích & xử lý lỗi](#9-lệnh-hữu-ích--xử-lý-lỗi)

---

## 1. Công cụ cần cài

| Công cụ | Phiên bản | Dùng để làm gì | Cài như nào |
|---------|-----------|----------------|-------------|
| **JDK** | 17+ (khuyến nghị 21) | Build & chạy server + client Android/PC | `apt install openjdk-21-jdk` (Linux) / [Adoptium](https://adoptium.net) (Win/Mac) |
| **Docker** + Compose | mới nhất | Chạy PostgreSQL + Redis, deploy | `curl -fsSL https://get.docker.com \| sh` |
| **Node.js** | 20+ | Build website + trang admin (Next.js) | [nodejs.org](https://nodejs.org) hoặc `nvm install 20` |
| **Gradle** | — | KHÔNG cần cài, đã có `./gradlew` sẵn | (tự tải khi chạy lần đầu) |
| **Flutter** | 3.19+ | Chỉ khi build client iOS | [flutter.dev](https://docs.flutter.dev/get-started/install) |
| **Apache Ant + WTK 2.5** | — | Chỉ khi build client J2ME (legacy) | `apt install ant` + Sun WTK |
| **Git** | — | Tải/đẩy mã nguồn | `apt install git` |

> Kiểm tra nhanh: `java -version` (>=17), `node -v` (>=20), `docker -v`, `git --version`.

---

## 2. Lấy mã nguồn & chạy thử local

```bash
git clone https://github.com/thanhtinz/vqsv.git
cd vqsv
```

**Bước 1 — Hạ tầng (PostgreSQL + Redis):**
```bash
docker compose up -d db redis
```
- PostgreSQL: `localhost:5432`, database `vqsv`, user `vqsv`, mật khẩu `vqsv123`.
- Redis: `localhost:6379`.

**Bước 2 — Server (Spring Boot):**
```bash
cd server
./gradlew bootRun
```
- REST API + WebSocket: `http://localhost:8080`
- Cổng TCP cho client game (J2ME/LibGDX): `9090`
- Flyway tự chạy migration tạo bảng + seed dữ liệu khi khởi động.

Kiểm tra: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.

**Bước 3 — Website người chơi:**
```bash
cd web && npm ci && npm run dev      # http://localhost:3000
```

**Bước 4 — Trang quản trị:**
```bash
cd admin && npm ci && npm run dev    # http://localhost:3001
```

**Bước 5 — Client game (PC):**
```bash
cd clients && ./gradlew :desktop:run
```

> Muốn chạy tất cả bằng Docker một phát: `docker compose up -d --build` (db + redis + server).

---

## 3. Cấu hình (config) mọi thành phần

### 3.1. Server — biến môi trường

Server đọc cấu hình từ biến môi trường (mặc định trong
`server/src/main/resources/application.yml`):

| Biến | Mặc định | Ý nghĩa |
|------|----------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/vqsv` | Chuỗi kết nối PostgreSQL |
| `DB_USER` / `DB_PASS` | `vqsv` / `vqsv123` | Tài khoản DB |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `REDIS_PASS` | (trống) | Mật khẩu Redis |
| `HTTP_PORT` | `8080` | Cổng REST/WebSocket |
| `TCP_PORT` | `9090` | Cổng game nhị phân |
| `JWT_SECRET` | (đổi ở production!) | Khóa ký JWT, **>= 32 ký tự** |
| `JWT_EXPIRATION_MS` | `86400000` | Hạn token (24h) |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | (trống) | Tạo/nâng quyền tài khoản admin lúc khởi động |

Cách đặt khi chạy local:
```bash
DB_URL=jdbc:postgresql://localhost:5432/vqsv \
ADMIN_USERNAME=admin ADMIN_PASSWORD=MatKhauManh123 \
./gradlew bootRun
```

### 3.2. Tạo tài khoản admin

1. Đặt `ADMIN_USERNAME` + `ADMIN_PASSWORD` rồi khởi động server →
   `AdminBootstrap` tự tạo tài khoản (hoặc nâng tài khoản sẵn có lên quyền `ADMIN`).
2. Mở trang admin (`http://localhost:3001`), đăng nhập bằng đúng user/mật khẩu đó.

### 3.3. Website & Admin — trỏ về server

Sửa biến `NEXT_PUBLIC_API_BASE` (xem `web/.env.example`, `admin/.env.example`):
```bash
# web/.env.local  và  admin/.env.local
NEXT_PUBLIC_API_BASE=http://localhost:8080        # local
# NEXT_PUBLIC_API_BASE=https://play.tengame.com   # production
```

### 3.4. Client game — trỏ về server

Mặc định client nối `localhost`. Đổi host server tại:

| Client | File | Sửa chỗ |
|--------|------|---------|
| PC | `clients/desktop/src/main/kotlin/com/vqsv/desktop/DesktopLauncher.kt` | `VqsvGame("localhost")` |
| Android | `clients/android/src/main/kotlin/com/vqsv/android/MainActivity.kt` | tham số host của `VqsvGame(...)` |
| iOS (Flutter) | `clients/ios/lib/services/api_service.dart`, `tcp_service.dart` | hằng host/baseUrl |
| J2ME | `clients/j2me/src/com/vqsv/j2me/screen/LoginScreen.java` | `SERVER_HOST` |

---

## 4. Đấu domain & deploy production

Chi tiết đầy đủ ở [`deploy/VPS_DEPLOY.md`](../deploy/VPS_DEPLOY.md). Tóm tắt:

**Bước 1 — Trỏ domain (DNS):** tạo 2 bản ghi **A record** về IP VPS:
```
play.tengame.com        A   <IP_VPS>     # website + API
admin.play.tengame.com  A   <IP_VPS>     # trang admin
```

**Bước 2 — Cấu hình secret trên VPS:**
```bash
cp .env.example .env
./scripts/gen-secrets.sh          # tự sinh mật khẩu DB/Redis/JWT
nano .env                         # đặt PUBLIC_DOMAIN và ACME_EMAIL
```

**Bước 3 — Mở tường lửa:** cổng 22, 80, 443, 9090.

**Bước 4 — Chạy:**
```bash
docker compose -f docker-compose.prod.yml up -d --build
```
Caddy tự xin chứng chỉ HTTPS. Sau đó:
- `https://play.tengame.com` → website + API (`/api/*`, `/ws`)
- `https://admin.play.tengame.com` → admin
- `play.tengame.com:9090` → cổng game (client trỏ host này)

**Cập nhật về sau:** `git pull && docker compose -f docker-compose.prod.yml up -d --build`.

---

## 5. Build client lên từng nền tảng

Tất cả lệnh chạy trong thư mục `clients/`. Asset game đã nằm sẵn trong
`clients/core/src/main/resources/game/` nên không cần thao tác gì thêm.

### PC (Windows / macOS / Linux)
```bash
cd clients
./gradlew :desktop:run                       # chạy thử
./gradlew :desktop:fatJar                     # đóng gói chạy độc lập
java -jar desktop/build/libs/desktop-all.jar
```

### Android (APK)
Cần Android SDK (đặt `ANDROID_HOME` hoặc `clients/local.properties`).
```bash
cd clients
./gradlew :android:assembleDebug
# Kết quả: android/build/outputs/apk/debug/android-debug.apk
adb install android/build/outputs/apk/debug/android-debug.apk
```

### iOS / Android (Flutter)
```bash
cd clients/ios
flutter pub get
flutter run            # chạy trên máy ảo/thiết bị
flutter build ipa      # bản iOS phát hành
flutter build apk      # bản Android phát hành
```

### J2ME (legacy)
```bash
cd clients/j2me
ant build              # tạo dist/vqsv.jar
```

> Trước khi build bản phát hành, nhớ đổi host server (mục 3.4) sang domain thật.

---

## 6. Thêm nội dung game

Có **2 cách**, dùng cách nào cũng được:

- **Cách A — Trang Admin (khuyên dùng, không cần code):** vào `http://localhost:3001`,
  mục tương ứng, bấm "Tạo", điền form, Lưu. Có hiệu lực ngay (không cần khởi động lại).
- **Cách B — SQL migration (để seed cố định, lên git):** thêm file
  `server/src/main/resources/db/migration/V{n+1}__ten_mo_ta.sql` rồi khởi động lại
  server → Flyway tự chạy. **Tuyệt đối không sửa file migration cũ** — luôn tạo số mới.

Tham chiếu nhanh bảng ↔ trang admin ↔ endpoint:

| Nội dung | Bảng | Trang admin | API |
|----------|------|-------------|-----|
| Bản đồ | `maps` | Game ▸ Bản đồ | `/api/admin/game/maps` |
| Mẫu sủng vật/quái | `pet_templates` | Game ▸ Sủng vật | `/api/admin/game/pets` |
| Quái hoang dã (spawn) | `map_wild_pets` | Game ▸ Wild Pets | `/api/admin/game/wild-pets` |
| NPC trên bản đồ | `npcs` | Game ▸ NPC | `/api/admin/game/npcs` |
| Trainer/Boss (đối thủ) | `npc_enemy_templates` | Game ▸ Enemies | `/api/admin/game/enemies` |
| Vật phẩm | `items` | Game ▸ Items | `/api/admin/game/items` |
| Cửa hàng | `shop_listings` | Game ▸ Shop | `/api/admin/game/shop` |
| Cổng dịch chuyển | `map_warps` | Game ▸ Warps | `/api/admin/game/warps` |

### 6.1. Thêm một BẢN ĐỒ

Cột: `name, width, height, tileset_id, bgm_id, min_level, is_pvp`.

SQL:
```sql
-- V6__them_ban_do_moi.sql
INSERT INTO maps (name, width, height, tileset_id, bgm_id, min_level, is_pvp) VALUES
  ('Hang Pha Lê', 30, 30, 4, 4, 40, FALSE);
```
Hoặc Admin ▸ Bản đồ ▸ Tạo. Ghi nhớ `id` map vừa tạo để dùng cho mob/npc/warp.

### 6.2. Thêm một MOB (quái) và cho nó xuất hiện

Mob gồm 2 phần: **mẫu** (`pet_templates`) + **điểm spawn trên map** (`map_wild_pets`).

1. Tạo mẫu sủng vật (cột chính: `name, sprite_id, element, base_hp, base_atk,
   base_def, base_spd, catch_rate, evolve_into, evolve_lv`):
```sql
INSERT INTO pet_templates (name, sprite_id, element, base_hp, base_atk, base_def, base_spd, catch_rate, evolve_into, evolve_lv)
VALUES ('Rồng Pha Lê', 50, 'WATER', 120, 18, 12, 10, 25, NULL, NULL);
-- lấy id vừa tạo, ví dụ = 13
```
2. Cho nó spawn trên map (cột: `map_id, template_id, min_level, max_level, spawn_rate`):
```sql
INSERT INTO map_wild_pets (map_id, template_id, min_level, max_level, spawn_rate)
VALUES (7, 13, 38, 45, 20);   -- map 7, mẫu 13, cấp 38-45, tỉ lệ gặp 20
```
Khi người chơi đi trên map đó sẽ ngẫu nhiên gặp mob này.

> `element` hợp lệ: `WOOD, EARTH, WATER, FIRE, GHOST, WIND, ELECTRIC` (alias cũ: `LIGHT`→ELECTRIC, `DARK`→GHOST). Bảng tương khắc xem `docs/ORIGINAL-MECHANICS.md`.

### 6.3. Thêm một NPC

Cột: `name, sprite_id, npc_type, map_id, pos_x, pos_y, dialog_key`.
`npc_type`: `DIALOG` (hội thoại), `SHOP` (cửa hàng), `BATTLE_TRAINER` (đấu).
```sql
INSERT INTO npcs (name, sprite_id, npc_type, map_id, pos_x, pos_y, dialog_key)
VALUES ('Ông Lão Bí Ẩn', 9, 'DIALOG', 7, 5, 8, 'lao_bi_an');
```
Hoặc Admin ▸ NPC ▸ Tạo.

### 6.4. Thêm một TRAINER (NPC đấu được)

Trainer = 1 NPC `BATTLE_TRAINER` + 1 đối thủ trong `npc_enemy_templates`, **liên kết
bằng `enemy_template_id`**. Người chơi đi tới cạnh NPC, bấm `G` để giao đấu.

```sql
-- 1) Đối thủ (cột: name, sprite_id, level, hp, atk, def, spd, exp_reward, gold_reward, map_id)
INSERT INTO npc_enemy_templates (name, sprite_id, level, hp, atk, def, spd, exp_reward, gold_reward, map_id)
VALUES ('Cao Thủ Pha Lê', 12, 42, 900, 100, 55, 35, 1500, 1000, 7);
-- giả sử id = 13

-- 2) NPC trainer trên map, trỏ tới đối thủ trên
INSERT INTO npcs (name, sprite_id, npc_type, map_id, pos_x, pos_y, enemy_template_id)
VALUES ('Cao Thủ Pha Lê', 12, 'BATTLE_TRAINER', 7, 10, 10, 13);
```
Thắng trận sẽ thưởng đúng `exp_reward` / `gold_reward`. (Xem mẫu thật ở
`V4__seed_npc_trainers.sql` và `V5__link_trainer_npcs.sql`.)

### 6.5. Thêm một VẬT PHẨM và bán trong shop

Cột item: `name, item_type, effect_val, icon_id, description`.
`item_type` server hiểu: `CATCH_BALL` (tất bắt thú, `effect_val` = lực bắt; >=100 = chắc chắn),
`MEDICINE` (hồi máu, `effect_val` = HP hồi), `LEVEL_UP`, `GOLD_PACK`.
```sql
-- 1) Vật phẩm
INSERT INTO items (name, item_type, effect_val, icon_id, description)
VALUES ('Tất Kim Cương', 'CATCH_BALL', 90, 4, 'Tỉ lệ bắt cực cao');
-- giả sử id = 12

-- 2) Bán trong cửa hàng (cột: item_id, price_gold, price_medal, sort_order)
INSERT INTO shop_listings (item_id, price_gold, price_medal, sort_order)
VALUES (12, 3000, NULL, 5);     -- bán 3000 vàng; để price_medal nếu bán bằng huy chương
```

### 6.6. Thêm CỔNG DỊCH CHUYỂN (warp) giữa 2 map

Cột: `from_map, from_x, from_y, to_map, to_x, to_y`.
```sql
INSERT INTO map_warps (from_map, from_x, from_y, to_map, to_x, to_y)
VALUES (1, 19, 10, 7, 0, 10);   -- đứng ô (19,10) map 1 → sang map 7 ô (0,10)
```

---

## 7. Thêm tính năng mới

Kiến trúc server (Spring Boot + Kotlin). Một tính năng thường đi qua các tầng:

```
entity/ (bảng)  →  repository/ (truy vấn)  →  migration (SQL tạo bảng)
        ↘
         service / game/ (logic)  →  controller/ (REST)  HOẶC  network/TcpGateway (giao thức game)
                                                   ↘
                                                    client (Op + TcpClient + screen)  /  web + admin
```

### Ví dụ thật: tính năng "Đấu NPC Trainer" đã làm trong repo

Tham khảo đúng các file này để bắt chước khi thêm tính năng tương tự:

1. **Bảng + entity:** cột `enemy_template_id` thêm vào `npcs`
   (`V5__link_trainer_npcs.sql`, `entity/GameContentEntities.kt`).
2. **Repository:** `repository/GameContentRepositories.kt`
   (`findByMapId` cho `NpcEnemyTemplateRepository`).
3. **Logic:** `game/battle/BattleService.kt` → hàm `startTrainerBattle(...)`
   (dựng trận PvE, không bắt được, thưởng cố định).
4. **Giao thức game (real-time):** `network/TcpGateway.kt`
   - thêm opcode `START_TRAINER = 0x0A` trong `object Op`
   - thêm `handleStartTrainer(...)` (kiểm tra người chơi đứng cạnh NPC mới cho đấu).
5. **Client:** `clients/core/src/main/kotlin/com/vqsv/core/net/Op.kt` (opcode),
   `net/TcpClient.kt` (`sendStartTrainer`), `screen/MapScreen.kt` (phím `G`, vẽ NPC).
6. **Test:** `server/src/test/kotlin/...` (vd `PvpServiceTest`).

### Checklist khi thêm tính năng

- [ ] Cần lưu dữ liệu? → tạo `entity/` + `repository/` + migration `V{n}__...sql`.
- [ ] Logic nghiệp vụ → thêm vào `service/` hoặc `game/`.
- [ ] Truy cập từ web/admin → thêm REST trong `controller/` hoặc `web/`.
- [ ] Hành động trong game (di chuyển, đánh...) → thêm opcode + handler trong
      `network/TcpGateway.kt`, rồi cập nhật client (`Op.kt`, `TcpClient.kt`, `screen/`).
- [ ] Giao thức nhị phân: client và server **phải khớp opcode + thứ tự byte**.
- [ ] Viết test (`./gradlew test`) và build client (`./gradlew :core:compileKotlin`).

> Giao thức TCP: gói tin = `[2 byte độ dài][1 byte opcode][payload]`, số big-endian.
> Đối chiếu danh sách opcode ở đầu `network/TcpGateway.kt` (server) và
> `core/net/Op.kt` (client) — hai bên phải trùng nhau.

---

## 8. Sprite ID & asset

Asset thật nằm trong `clients/core/src/main/resources/game/`:

| Thư mục | Nội dung |
|---------|----------|
| `png/img/img_<id>.png` | ảnh atlas (theo `sprite_id`) |
| `spr/spr_<id>.json` | bảng sprite (module/frame/animation) |
| `map/map_<id>.json` | dữ liệu tile map |
| `meta/sprite_table.json` | ánh xạ sprite id → file ảnh |

Khi đặt `sprite_id` cho mob/npc/item, dùng id có thật trong `png/img/`. Muốn xem
nhanh có những id nào: liệt kê thư mục đó.

**Tạo lại asset từ JAR game mới (hiếm khi cần):**
```bash
python3 tools/asset-extractor/extract.py path/to/game.jar clients/core/src/main/resources/game
```
(JAR gốc không lưu trong repo — bạn tự cung cấp.)

---

## 9. Lệnh hữu ích & xử lý lỗi

```bash
# Server
cd server && ./gradlew bootRun        # chạy
cd server && ./gradlew test           # chạy test
cd server && ./gradlew bootJar        # đóng gói jar

# Client
cd clients && ./gradlew :desktop:run
cd clients && ./gradlew :android:assembleDebug

# Web / Admin
cd web && npm run dev | npm run build
cd admin && npm run dev | npm run build

# Hạ tầng
docker compose up -d db redis         # bật DB + Redis
docker compose logs -f server         # xem log
docker compose down                   # tắt
```

**Lỗi thường gặp:**

- *Server không kết nối DB* → kiểm tra `docker compose ps` (db `healthy` chưa),
  và `DB_URL/DB_USER/DB_PASS` đúng chưa.
- *Migration lỗi / "checksum mismatch"* → bạn đã sửa file migration cũ. Hoàn nguyên
  file đó và tạo file `V{n+1}__...sql` mới thay vì sửa.
- *Client không vào được server* → sai host (mục 3.4) hoặc cổng `9090` chưa mở
  (production cần mở firewall 9090).
- *Web/Admin gọi API lỗi CORS/timeout* → sai `NEXT_PUBLIC_API_BASE`.
- *Admin đăng nhập không được* → chưa đặt `ADMIN_USERNAME/ADMIN_PASSWORD` khi khởi
  động server, hoặc đăng nhập sai user vừa đặt.
- *Đổi nội dung game không thấy* → nếu sửa qua SQL phải khởi động lại server; sửa
  qua Admin thì có hiệu lực ngay (bấm Lưu rồi tải lại trang).
```
