# Hướng dẫn phát triển VQSV

Tài liệu cho lập trình viên: chạy toàn bộ hệ thống ở máy local, cấu trúc dự án,
và quy trình build/test. Để triển khai production lên VPS, xem
[`deploy/VPS_DEPLOY.md`](../deploy/VPS_DEPLOY.md).

## Yêu cầu

| Thành phần | Cần có |
|-----------|--------|
| Server | JDK 17+ (khuyến nghị 21), Docker (cho PostgreSQL + Redis) |
| Web / Admin | Node.js 20+ |
| Client (Android/PC) | JDK 17+ (Gradle wrapper tự lo phần còn lại) |
| Client iOS | Flutter 3.19+ |
| Client J2ME | Apache Ant + WTK 2.5 (tuỳ chọn, bản legacy) |

> Không cần cài sẵn Gradle — mỗi module có `./gradlew` (Gradle wrapper).

## 1. Hạ tầng (PostgreSQL + Redis)

```bash
docker compose up -d db redis
```

Mặc định: PostgreSQL `localhost:5432` (db `vqsv`, user `vqsv`, pass `vqsv123`),
Redis `localhost:6379`. Server đọc các giá trị này qua biến môi trường (xem
`server/src/main/resources/application.yml`).

## 2. Server (Spring Boot + Kotlin)

```bash
cd server
./gradlew bootRun          # chạy server: REST :8080, WebSocket :8080/ws, TCP :9090
./gradlew test             # chạy unit test
./gradlew bootJar          # đóng gói build/libs/vqsv-server-1.0.0.jar
```

Flyway tự chạy migration khi khởi động (`src/main/resources/db/migration`).
Hoặc chạy cả stack bằng Docker: `docker compose up -d --build`.

## 3. Website người chơi (Next.js)

```bash
cd web
npm ci
npm run dev                # http://localhost:3000
```

Đặt `NEXT_PUBLIC_API_BASE` (xem `web/.env.example`) trỏ tới server, mặc định
`http://localhost:8080`.

## 4. Trang quản trị (Next.js)

```bash
cd admin
npm ci
npm run dev                # http://localhost:3001
```

## 5. Client game

Xem chi tiết từng nền tảng trong [`clients/README.md`](../clients/README.md). Tóm tắt:

```bash
cd clients
./gradlew :desktop:run             # chạy bản PC (LibGDX)
./gradlew :android:assembleDebug   # build APK Android (cần Android SDK)
```

## Cấu trúc dự án

```
vqsv/
├── server/     Spring Boot + Kotlin — REST, WebSocket, TCP gateway (Netty), JPA, Flyway
│   ├── src/main/kotlin/com/vqsv/
│   │   ├── controller/   REST controllers
│   │   ├── web/          controller + service cho web/admin, multi-server
│   │   ├── service/      logic nghiệp vụ (auth...)
│   │   ├── game/         battle / map / pet / item
│   │   ├── network/      TcpGateway (giao thức nhị phân J2ME)
│   │   ├── entity/ repository/ dto/ util/ config/
│   │   └── ...
│   └── src/main/resources/db/migration/   Flyway SQL (V1..Vn)
├── clients/    Client game (LibGDX + Flutter + J2ME)
│   ├── core/       code dùng chung (net, model, asset, screen) + asset trong src/main/resources/game
│   ├── android/ desktop/   launcher mỏng dùng core
│   ├── ios/        bản Flutter
│   └── j2me/       bản J2ME gốc
├── web/        Website người chơi (Next.js + TypeScript + Tailwind)
├── admin/      Trang quản trị (Next.js)
├── tools/      asset-extractor: giải mã asset/dữ liệu từ JAR gốc
├── deploy/     Hướng dẫn + script triển khai VPS (Caddy, backup)
└── docs/       Tài liệu kỹ thuật
```

## Test

```bash
cd server && ./gradlew test     # unit test server (vd: GameFormula)
cd web    && npm run build       # type-check + build
cd admin  && npm run build
```

CI (`.github/workflows/ci.yml`) chạy tất cả các bước trên cho mỗi push/PR vào `main`.

## Quy ước

- `.editorconfig` quy định style chung (Kotlin/Java 4 space, TS/Dart 2 space, LF).
- Migration không sửa file cũ — luôn thêm `V{n+1}__...sql` mới.
- Asset là file thật trong `clients/core/src/main/resources/game/`; muốn cập nhật
  thì chạy lại `tools/asset-extractor/extract.py` rồi commit (xem README của tool).
