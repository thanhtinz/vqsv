# VQSV — Hướng dẫn đầy đủ (cho người mới bắt đầu)

> Đọc lần đầu thấy nhiều? Đừng lo. Cứ làm **đúng theo thứ tự** trong tài liệu này,
> mỗi bước đều có "kết quả mong đợi" để bạn biết mình làm đúng chưa.

---

## 0. Hệ thống VQSV là gì? (giải thích dễ hiểu)

Game này giống như **một quán ăn online**:

| Thành phần | Giống cái gì | Việc của nó |
|------------|--------------|-------------|
| **Server** | Cái bếp + người quản lý quán | Xử lý mọi thứ: đăng nhập, đánh nhau, lưu tiến trình. Chạy trên một máy chủ. |
| **Database** (PostgreSQL) | Cuốn sổ ghi chép của quán | Lưu tài khoản, sủng vật, vật phẩm... Không mất khi tắt máy. |
| **Redis** | Bảng nhớ tạm dán trên tường | Lưu phiên đăng nhập, dữ liệu tạm — cho nhanh. |
| **Client** | Khách tới quán | Cái mà người chơi cầm: app điện thoại / game PC. Nói chuyện với Server. |
| **Website** | Trang giới thiệu quán | Cho người chơi xem tin tức, bảng xếp hạng, nạp thẻ. |
| **Trang Admin** | Phòng quản lý | Cho bạn (chủ game) thêm map, vật phẩm, xem người chơi... |
| **Domain** | Địa chỉ nhà của quán | Ví dụ `play.tengame.com` — để người chơi tìm tới server. |

Tất cả nằm trong một thư mục mã nguồn. Bạn sẽ: **cài vài công cụ → bật server lên →
mở client/website → (tuỳ chọn) đưa lên internet bằng domain**.

### Thuật ngữ hay gặp (đọc lướt cũng được)

- **Terminal / Command line**: cửa sổ gõ lệnh. Trên Windows là **PowerShell**, trên
  Mac/Linux là **Terminal**. Mọi lệnh `như thế này` đều gõ ở đây.
- **Port (cổng)**: như số phòng. Server mở cổng `8080` (web/API) và `9090` (game).
- **Build**: "đóng gói" mã nguồn thành app chạy được (file `.apk`, `.jar`...).
- **Migration**: file chứa lệnh tạo/sửa bảng trong database. Server tự chạy khi khởi động.
- **Environment variable (biến môi trường)**: ô cấu hình truyền vào lúc chạy, ví dụ
  mật khẩu database — để không phải ghi cứng trong code.

---

## 1. Cài công cụ (làm 1 lần)

Cài các thứ sau (chỉ cài cái bạn cần):

| Công cụ | Bắt buộc? | Để làm gì | Cài ở đâu |
|---------|-----------|-----------|-----------|
| **Java (JDK) 17+** | ✅ Có | Chạy server & build game | [adoptium.net](https://adoptium.net) → tải "Temurin 21" |
| **Docker Desktop** | ✅ Có | Chạy database + Redis dễ dàng | [docker.com](https://www.docker.com/products/docker-desktop/) |
| **Node.js 20+** | ✅ Có | Chạy website + trang admin | [nodejs.org](https://nodejs.org) → bản "LTS" |
| **Git** | ✅ Có | Tải mã nguồn về | [git-scm.com](https://git-scm.com) |
| **Flutter** | ❌ Chỉ khi làm app iOS | Build client iOS | [docs.flutter.dev](https://docs.flutter.dev/get-started/install) |

> **Gradle KHÔNG cần cài** — dự án đã có sẵn lệnh `./gradlew` tự lo.

**Kiểm tra đã cài đúng chưa** — mở Terminal/PowerShell gõ từng dòng:
```bash
java -version      # phải hiện 17 trở lên
node -v            # phải hiện v20 trở lên
docker -v          # phải hiện số phiên bản
git --version      # phải hiện số phiên bản
```
*Kết quả mong đợi:* mỗi lệnh in ra một dòng phiên bản. Nếu báo "not found / không tìm
thấy" → cài lại công cụ đó rồi mở Terminal mới.

---

## 2. Chạy lần đầu (làm đúng thứ tự này)

### Bước 1 — Tải mã nguồn về
```bash
git clone https://github.com/thanhtinz/vqsv.git
cd vqsv
```
*Kết quả:* có thư mục `vqsv`, bạn đang đứng trong đó.

### Bước 2 — Bật Database + Redis
Mở **Docker Desktop** trước (đợi nó báo "running"), rồi gõ:
```bash
docker compose up -d db redis
```
*Kết quả mong đợi:* `docker compose ps` thấy `vqsv-db` và `vqsv-redis` đều `healthy`.
- Database PostgreSQL: chạy ở `localhost:5432` (tên db `vqsv`, user `vqsv`, mật khẩu `vqsv123`).

### Bước 3 — Bật Server
```bash
cd server
./gradlew bootRun
```
(Trên Windows dùng `gradlew bootRun`, không có `./`.)

Lần đầu sẽ tải thư viện, hơi lâu (vài phút). *Kết quả mong đợi:* dòng cuối hiện
`Started VqsvApplicationKt`. Server đang chạy — **cứ để cửa sổ này mở**.

Kiểm tra ở cửa sổ Terminal **khác**:
```bash
curl http://localhost:8080/actuator/health
```
*Kết quả mong đợi:* `{"status":"UP"}`.

### Bước 4 — (Tuỳ chọn) Bật Website
Mở Terminal mới:
```bash
cd vqsv/web
npm ci            # cài thư viện (làm 1 lần)
npm run dev       # mở http://localhost:3000
```

### Bước 5 — (Tuỳ chọn) Bật Trang Admin
```bash
cd vqsv/admin
npm ci
npm run dev       # mở http://localhost:3001
```

### Bước 6 — (Tuỳ chọn) Chạy thử Game trên PC
```bash
cd vqsv/clients
./gradlew :desktop:run
```
*Kết quả mong đợi:* cửa sổ game hiện ra, vào được màn đăng nhập.

> 🎉 Tới đây bạn đã có cả server + game + web + admin chạy trên máy mình.

---

## 3. Cấu hình (chỉnh cho hợp ý bạn)

### 3.1. Đổi cấu hình Server
Server nhận cấu hình qua **biến môi trường** (không cần sửa code). Ví dụ chạy server
kèm tạo tài khoản admin:
```bash
ADMIN_USERNAME=admin ADMIN_PASSWORD=MatKhauManh123 ./gradlew bootRun
```
*(Windows PowerShell:* `$env:ADMIN_USERNAME="admin"; $env:ADMIN_PASSWORD="MatKhauManh123"; ./gradlew bootRun`)*

Các cấu hình hay dùng:

| Biến | Mặc định | Ý nghĩa |
|------|----------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/vqsv` | Địa chỉ database |
| `DB_USER` / `DB_PASS` | `vqsv` / `vqsv123` | Tài khoản database |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `HTTP_PORT` | `8080` | Cổng web/API |
| `TCP_PORT` | `9090` | Cổng game |
| `JWT_SECRET` | (nên đổi khi lên mạng!) | Khoá bảo mật đăng nhập, **>= 32 ký tự** |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | (trống) | Tạo/nâng quyền tài khoản admin |

### 3.2. Tạo & dùng tài khoản Admin
1. Chạy server có đặt `ADMIN_USERNAME` + `ADMIN_PASSWORD` như trên.
2. Mở `http://localhost:3001`, đăng nhập bằng đúng tên/mật khẩu đó.
*Kết quả:* vào được bảng điều khiển admin.

### 3.3. Cho Website/Admin biết server ở đâu
Tạo file `web/.env.local` (và `admin/.env.local`) với 1 dòng:
```bash
NEXT_PUBLIC_API_BASE=http://localhost:8080
# Khi lên mạng đổi thành: https://play.tengame.com
```

### 3.4. Cho Client (game) biết server ở đâu
Mặc định game nối `localhost`. Khi muốn nối server thật, sửa địa chỉ trong file:

| Game trên | Sửa file này | Tìm dòng |
|-----------|--------------|----------|
| PC | `clients/desktop/.../DesktopLauncher.kt` | `VqsvGame("localhost")` → đổi `localhost` |
| Android | `clients/android/.../MainActivity.kt` | tham số host của `VqsvGame(...)` |
| iOS (Flutter) | `clients/ios/lib/services/api_service.dart`, `tcp_service.dart` | địa chỉ host |
| J2ME | `clients/j2me/.../LoginScreen.java` | `SERVER_HOST` |

---

## 4. Đưa game lên internet (đấu domain & deploy)

Bạn cần: **1 VPS** (máy chủ thuê ngoài) + **1 domain** (tên miền đã mua).
Hướng dẫn chi tiết: [`deploy/VPS_DEPLOY.md`](../deploy/VPS_DEPLOY.md). Tóm tắt 4 bước:

**Bước 1 — Trỏ domain về VPS (làm ở trang quản lý tên miền):** tạo 2 bản ghi **A**:
```
play.tengame.com        →  <địa chỉ IP của VPS>     (website + game)
admin.play.tengame.com  →  <địa chỉ IP của VPS>     (trang admin)
```
*(thay `tengame.com` bằng domain của bạn)*

**Bước 2 — Trên VPS, lấy mã nguồn rồi tạo cấu hình bí mật:**
```bash
git clone https://github.com/thanhtinz/vqsv.git && cd vqsv
cp .env.example .env
./scripts/gen-secrets.sh     # tự tạo mật khẩu database/redis/JWT mạnh
nano .env                    # điền PUBLIC_DOMAIN và ACME_EMAIL của bạn
```

**Bước 3 — Mở tường lửa** cho các cổng: `22` (SSH), `80`, `443` (web), `9090` (game).

**Bước 4 — Chạy:**
```bash
docker compose -f docker-compose.prod.yml up -d --build
```
*Kết quả mong đợi:* sau ít phút, vào `https://play.tengame.com` thấy website (đã có
HTTPS tự động). Client game thì trỏ host `play.tengame.com` cổng `9090`.

**Cập nhật game sau này:**
```bash
git pull && docker compose -f docker-compose.prod.yml up -d --build
```

---

## 5. Đóng gói game cho người chơi (build client)

Chạy trong thư mục `clients/`. Asset (hình ảnh, map) đã nằm sẵn trong mã nguồn, không
cần làm gì thêm. **Nhớ đổi host server (mục 3.4) trước khi build bản phát hành.**

| Nền tảng | Lệnh | Ra file gì |
|----------|------|-----------|
| **PC** | `./gradlew :desktop:fatJar` | `desktop/build/libs/desktop-all.jar` (chạy: `java -jar ...`) |
| **Android** | `./gradlew :android:assembleDebug` | `android/build/outputs/apk/debug/android-debug.apk` |
| **iOS** | `cd ios && flutter build ipa` | bản cài iOS |
| **J2ME** | `cd j2me && ant build` | `dist/vqsv.jar` |

> Android cần cài Android SDK. iOS cần máy Mac + Flutter.

---

## 6. Thêm nội dung game (map, mob, NPC, vật phẩm...)

Có **2 cách**. Người mới nên dùng **Cách A**.

> **Cách A — Trang Admin (dễ, không cần code):** mở `http://localhost:3001`, vào mục
> tương ứng, bấm nút **"Tạo"**, điền form, bấm **"Lưu"**. Có hiệu lực ngay.
>
> **Cách B — Viết file SQL (để lưu cố định vào mã nguồn):** tạo file mới
> `server/src/main/resources/db/migration/V{số kế tiếp}__mo_ta.sql` rồi khởi động lại
> server. **Không bao giờ sửa file SQL cũ — luôn tạo số mới** (V6, V7, V8...).

Bảng tra cứu (mục Admin ↔ bảng database):

| Muốn thêm | Vào Admin mục | Bảng |
|-----------|---------------|------|
| Bản đồ | Bản đồ | `maps` |
| Mob / sủng vật (mẫu) | Sủng vật | `pet_templates` |
| Cho mob xuất hiện trên map | Wild Pets | `map_wild_pets` |
| NPC đứng trên map | NPC | `npcs` |
| Đối thủ Trainer/Boss | Enemies | `npc_enemy_templates` |
| Vật phẩm | Items | `items` |
| Bán đồ trong cửa hàng | Shop | `shop_listings` |
| Cổng dịch chuyển | Warps | `map_warps` |

Dưới đây ví dụ bằng **Cách B (SQL)** — ai dùng Admin thì điền đúng các ô tương ứng.

### 6.1. Thêm một BẢN ĐỒ
Ô cần điền: tên, rộng, cao, tileset, nhạc nền, cấp tối thiểu, cho PvP không.
```sql
INSERT INTO maps (name, width, height, tileset_id, bgm_id, min_level, is_pvp)
VALUES ('Hang Pha Lê', 30, 30, 4, 4, 40, FALSE);
```
Ghi nhớ **id** của map vừa tạo (ví dụ `7`) để dùng cho mob/npc bên dưới.

### 6.2. Thêm một MOB (quái) + cho nó xuất hiện
Mob gồm **2 phần**: tạo *mẫu sủng vật*, rồi *cho nó spawn trên map*.
```sql
-- (1) Mẫu sủng vật
INSERT INTO pet_templates (name, sprite_id, element, base_hp, base_atk, base_def, base_spd, catch_rate, evolve_into, evolve_lv)
VALUES ('Rồng Pha Lê', 50, 'WATER', 120, 18, 12, 10, 25, NULL, NULL);
-- giả sử mẫu này có id = 13

-- (2) Cho nó xuất hiện trên map 7, cấp 38-45, tỉ lệ gặp 20
INSERT INTO map_wild_pets (map_id, template_id, min_level, max_level, spawn_rate)
VALUES (7, 13, 38, 45, 20);
```
`element` chọn 1 trong: `WOOD, EARTH, WATER, FIRE, GHOST, WIND, ELECTRIC`.

### 6.3. Thêm một NPC
`npc_type`: `DIALOG` (nói chuyện), `SHOP` (mở cửa hàng), `BATTLE_TRAINER` (đấu).
```sql
INSERT INTO npcs (name, sprite_id, npc_type, map_id, pos_x, pos_y, dialog_key)
VALUES ('Ông Lão Bí Ẩn', 9, 'DIALOG', 7, 5, 8, 'lao_bi_an');
```

### 6.4. Thêm một TRAINER (NPC đấu được)
Trainer = 1 NPC loại `BATTLE_TRAINER` + 1 đối thủ, nối với nhau bằng `enemy_template_id`.
Trong game, người chơi đi tới **cạnh** NPC rồi bấm **`G`** để đấu.
```sql
-- (1) Đối thủ
INSERT INTO npc_enemy_templates (name, sprite_id, level, hp, atk, def, spd, exp_reward, gold_reward, map_id)
VALUES ('Cao Thủ Pha Lê', 12, 42, 900, 100, 55, 35, 1500, 1000, 7);
-- giả sử id = 13

-- (2) NPC trainer trỏ tới đối thủ trên
INSERT INTO npcs (name, sprite_id, npc_type, map_id, pos_x, pos_y, enemy_template_id)
VALUES ('Cao Thủ Pha Lê', 12, 'BATTLE_TRAINER', 7, 10, 10, 13);
```
(Mẫu thật xem `V4__seed_npc_trainers.sql` + `V5__link_trainer_npcs.sql`.)

### 6.5. Thêm VẬT PHẨM + bán trong cửa hàng
`item_type` server hiểu: `CATCH_BALL` (tất bắt thú, `effect_val`=lực bắt, ≥100 = chắc
chắn), `MEDICINE` (hồi máu, `effect_val`=HP hồi), `LEVEL_UP`, `GOLD_PACK`.
```sql
-- (1) Vật phẩm
INSERT INTO items (name, item_type, effect_val, icon_id, description)
VALUES ('Tất Kim Cương', 'CATCH_BALL', 90, 4, 'Tỉ lệ bắt cực cao');
-- giả sử id = 12

-- (2) Bán 3000 vàng trong shop (để price_medal nếu muốn bán bằng huy chương)
INSERT INTO shop_listings (item_id, price_gold, price_medal, sort_order)
VALUES (12, 3000, NULL, 5);
```

### 6.6. Thêm CỔNG DỊCH CHUYỂN giữa 2 map
```sql
-- Đứng ô (19,10) ở map 1 sẽ sang map 7 tại ô (0,10)
INSERT INTO map_warps (from_map, from_x, from_y, to_map, to_x, to_y)
VALUES (1, 19, 10, 7, 0, 10);
```

> **Lưu ý:** sửa bằng **SQL** thì phải khởi động lại server; sửa bằng **Admin** thì có
> hiệu lực ngay (bấm Lưu rồi tải lại trang).

---

## 7. Thêm tính năng mới (dành cho người biết code)

Một tính năng thường đi qua các tầng trong `server/src/main/kotlin/com/vqsv/`:

```
entity/      (định nghĩa bảng)
repository/   (truy vấn database)
migration     (file SQL tạo bảng)
service/ game/ (logic: tính toán, luật chơi)
controller/   (mở API cho web/admin)   HOẶC   network/TcpGateway.kt (lệnh trong game)
        ↓
client (core/net/Op.kt + TcpClient.kt + screen/)   /   web + admin
```

**Ví dụ có thật trong repo — tính năng "Đấu NPC Trainer":** muốn làm tính năng tương tự,
xem đúng các file sau để bắt chước:
1. Bảng: `V5__link_trainer_npcs.sql` + `entity/GameContentEntities.kt`
2. Truy vấn: `repository/GameContentRepositories.kt`
3. Luật chơi: `game/battle/BattleService.kt` (hàm `startTrainerBattle`)
4. Lệnh trong game: `network/TcpGateway.kt` (opcode `START_TRAINER`, hàm `handleStartTrainer`)
5. Client: `clients/core/.../net/Op.kt`, `net/TcpClient.kt`, `screen/MapScreen.kt`
6. Test: `server/src/test/kotlin/.../PvpServiceTest.kt`

**Checklist:**
- [ ] Cần lưu dữ liệu? → thêm `entity/` + `repository/` + file migration mới.
- [ ] Logic → `service/` hoặc `game/`.
- [ ] Dùng từ web/admin → thêm API trong `controller/`.
- [ ] Thao tác trong game → thêm opcode + handler ở `network/TcpGateway.kt`, rồi cập
      nhật client (`Op.kt`, `TcpClient.kt`, `screen/`). **Opcode 2 bên phải khớp nhau.**
- [ ] Chạy `./gradlew test` và build thử client.

---

## 8. Sprite ID & hình ảnh

Khi đặt `sprite_id` cho mob/npc/vật phẩm, dùng id **có thật** trong thư mục ảnh:
`clients/core/src/main/resources/game/png/img/img_<id>.png`.
Muốn biết có những id nào → mở thư mục đó xem tên file.

Tạo lại toàn bộ asset từ một file game JAR mới (hiếm khi cần):
```bash
python3 tools/asset-extractor/extract.py duong-dan/game.jar clients/core/src/main/resources/game
```

---

## 9. Lỗi thường gặp & cách sửa

| Triệu chứng | Nguyên nhân & cách sửa |
|-------------|------------------------|
| Lệnh báo `not found` | Chưa cài công cụ hoặc chưa mở Terminal mới sau khi cài. |
| Server không lên, lỗi kết nối DB | Docker chưa chạy, hoặc `docker compose up -d db redis` chưa làm. Kiểm tra `docker compose ps`. |
| `checksum mismatch` khi khởi động | Bạn đã **sửa một file migration cũ**. Hoàn nguyên nó, tạo file `V{n+1}__...sql` mới. |
| Client không vào được server | Sai địa chỉ host (mục 3.4) hoặc cổng `9090` chưa mở (production phải mở firewall 9090). |
| Web/Admin gọi API lỗi | Sai `NEXT_PUBLIC_API_BASE`. |
| Đăng nhập admin không được | Chưa đặt `ADMIN_USERNAME/ADMIN_PASSWORD` khi chạy server, hoặc gõ sai. |
| Thêm nội dung không thấy đổi | Sửa bằng SQL phải khởi động lại server; sửa bằng Admin thì tải lại trang. |

Còn vướng ở đâu, ghi rõ bạn đang ở **bước nào** và **thông báo lỗi** hiện ra để được hỗ trợ.
