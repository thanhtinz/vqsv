# Vương Quốc Sủng Vật Online (VQSV)

Reverse-engineered và rebuild thành game online multiplayer từ bản J2ME gốc.

## Kiến trúc

```
┌─────────────────────────────────────────────────────┐
│                   CLIENTS                           │
│  J2ME (JAR)  │  Android  │  iOS  │  Web (React)    │
└──────┬────────────┬───────────────────┬─────────────┘
       │ TCP:9090   │   HTTP/WS:8080    │
       │ Binary     │   REST + STOMP    │
┌──────▼────────────▼───────────────────▼─────────────┐
│              SPRING BOOT SERVER                     │
│  AuthService │ MapService │ BattleService           │
│  PetService  │ ShopService│ TcpGateway (Netty)      │
│  WebSocket (STOMP)   │  REST API                   │
└──────────────────────┬──────────────────────────────┘
                       │
         ┌─────────────┴──────────────┐
         ▼                            ▼
    PostgreSQL 16               Redis 7
    (game data)             (session cache)
```

## Stack
- **Server**: Spring Boot 3.2 + Kotlin, Netty TCP gateway
- **DB**: PostgreSQL 16 + Flyway migration
- **Cache**: Redis 7
- **Protocol**: REST JSON (web/mobile) + Binary TCP (J2ME gốc)
- **Realtime**: STOMP over WebSocket

## Khởi động nhanh

```bash
# 1. Start infrastructure
docker-compose up -d db redis

# 2. Build & run server
cd server
./gradlew bootRun

# Hoặc full docker:
docker-compose up -d
```

## Ports
| Port | Dịch vụ |
|------|----------|
| 8080 | REST API + WebSocket (web/Android/iOS) |
| 9090 | J2ME Binary TCP gateway |
| 5432 | PostgreSQL |
| 6379 | Redis |

## API Endpoints

### Auth
```
POST /api/auth/register   Body: {username, password, email?, playerName}
POST /api/auth/login      Body: {username, password}
```

### Player (cần Bearer token)
```
GET  /api/player/map      -> MapStateDto
POST /api/player/move     Body: {direction: "UP|DOWN|LEFT|RIGHT"}
```

### Pet
```
GET  /api/pets                       -> List<PetDto>
POST /api/pets/{petId}/heal?itemId=  -> PetDto
POST /api/pets/{petId}/evolve        -> PetDto
POST /api/pets/{petId}/slot?slot=    -> 204
```

### Battle
```
POST /api/battle/action   Body: {battleId, action: "ATTACK|USE_ITEM|CATCH|RUN", itemId?}
```

### Shop
```
GET  /api/shop            -> List<ShopItemDto>
GET  /api/shop/inventory  -> List<InventoryItemDto>
POST /api/shop/buy        Body: {shopListingId, quantity}
```

## Gameplay (tái tạo từ bản gốc)

### Sủng vật
- 12 loại sủng vật, 6 nguyên tố (Lửa/Nước/Gió/Đất/Sáng/Tối)
- Cơ chế bất lợi/lợi thế nguyên tố (x1.5 / x0.67)
- Level 1-100, hệ thống tiến hóa (level 20-25)
- Chỉ số: HP, ATK, DEF, SPD tăng theo growth rate

### Chiến đấu
- Turn-based, tốc độ quyết định ai đánh trước
- Hành động: Tấn công / Dùng vật phẩm / Bắt / Chạy
- Chí mạng dựa trên SPD
- Bắt sủng vật với Tất (tỷ lệ theo HP còn lại + loại Tất)

### Map
- 6 map từ Làng Khởi Đầu đến Tháp Bóng Tối
- Đi bộ kích hoạt random encounter
- Online players hiển thị real-time qua WebSocket

### Tiền tệ
- Kim Tiền: mua từ shop, nhận từ chiến đấu
- Huy Chương: mua vật phẩm hiếm

### Thay thế SMS gốc
| SMS gốc | Thay bằng |
|---------|-----------|
| 15,000đ kích hoạt | Đăng ký tài khoản miễn phí |
| 10,000đ Tất trúng cầu | Mua bằng Huy Chương trong shop |
| 10,000đ kim tiền | Mua Gói Kim Tiền trong shop |
| 10,000đ tăng cấp | Dùng Kẹo Tăng Cấp |
| 10,000đ huy hiệu | Chiến đấu để unlock badge |

## Client platforms
| Platform | File |
|----------|---------|
| J2ME (gốc) | `vqsv-360x640.jar` - connect TCP port 9090 |
| Android | `client-android/README.md` |
| iOS | dùng REST API + STOMP (Swift/Flutter) |
| Web | dùng REST API + STOMP (React/Vue) |

---

## Client Platforms

| Platform | Language | Directory | Build Tool |
|----------|----------|-----------|------------|
| **Android** | Kotlin + LibGDX | `clients/android/` | Gradle |
| **PC Desktop** | Kotlin + LibGDX + LWJGL3 | `clients/desktop/` | Gradle |
| **iOS** | Flutter + Dart | `client-ios/` | Flutter CLI |
| **J2ME (Legacy)** | Java ME MIDP 2.0 | `client-j2me/` | Apache Ant |

### Architecture

All modern clients (Android, PC, iOS) share the same game protocol implementation:
- **TCP Binary** (port 9090) — primary low-latency game channel for login/move/battle
- **REST HTTP** (port 8080) — initial auth, shop, pet management
- **WebSocket STOMP** (port 8080/ws) — real-time map events (player positions, chat)

The Android and PC clients share a common LibGDX core module (`clients/core/`) for game logic and rendering.

### Build & Run

**Prerequisites:** Java 17+, Android SDK (for Android), Flutter 3.19+ (for iOS)

**Android:**
```bash
cd clients
./gradlew :android:assembleDebug
# Install on connected device/emulator:
adb install android/build/outputs/apk/debug/android-debug.apk
```

**PC Desktop:**
```bash
cd clients
./gradlew :desktop:run
# Or build a standalone JAR:
./gradlew :desktop:fatJar
java -jar desktop/build/libs/desktop-all.jar
```

**iOS / Flutter (also runs on Android):**
```bash
cd client-ios
flutter pub get
flutter run                    # iOS Simulator or Android device
flutter build ipa              # Production iOS IPA
flutter build apk              # Production Android APK
```

**J2ME Legacy (requires WTK 2.5):**
```bash
cd client-j2me
# Edit src/com/vqsv/j2me/screen/LoginScreen.java and set SERVER_HOST
ant build
# Run in emulator:
java -jar /path/to/microemulator.jar dist/vqsv.jar
```

### Orientation

All modern clients default to **landscape (800×480)**.  
The J2ME client preserves the original **portrait (360×640)** layout.  
Converting portrait to landscape only requires adjusting UI layout coordinates — sprite assets and map tiles are orientation-agnostic.

---

## Game Assets

The original game's art and data live in custom binary containers inside the
J2ME JAR. An asset pipeline converts them to open formats for the modern clients.

| Step | Path |
|------|------|
| Original JAR (source of truth) | `assets/original/vqsv-original.jar` |
| Extractor / converter | `tools/asset-extractor/extract.py` |
| Format reference | `docs/ASSET-FORMATS.md` |

```bash
cd tools/asset-extractor
python3 extract.py ../../assets/original/vqsv-original.jar out
#  -> 309 PNG (img, byte-exact) + spr/map/ui JSON
```

Inventory: **309** images (PNG), **345** sprite tables, **102** tile maps (23×23),
**44** UI layouts. Only the JAR + tool are version-controlled; derived assets are
regenerated on demand, so a game update is just "replace the JAR, re-run the tool".
See [`assets/original/README.md`](assets/original/README.md) for how to add the JAR.
