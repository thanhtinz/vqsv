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
