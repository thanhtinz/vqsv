# VQSV Clients

Client game cho nhiều nền tảng, tất cả nói cùng một giao thức với server:

- **TCP nhị phân** (cổng 9090) — kênh game chính (đăng nhập, di chuyển, chiến đấu).
- **REST HTTP** (cổng 8080) — auth, cửa hàng, quản lý sủng vật.
- **WebSocket STOMP** (8080/ws) — sự kiện thời gian thực (vị trí người chơi, chat).

## Cấu trúc

| Thư mục | Nền tảng | Công nghệ |
|---------|----------|-----------|
| `core/` | Code dùng chung | Kotlin + LibGDX (net, model, asset, screen) |
| `android/` | Android | LibGDX (wrapper mỏng dùng `core`) |
| `desktop/` | PC (Win/Mac/Linux) | LibGDX + LWJGL3 |
| `ios/` | iOS | Flutter / Dart (độc lập) |
| `j2me/` | Điện thoại Java (legacy) | Java ME MIDP 2.0 + Ant |

Android, Desktop và Core build chung qua Gradle (`settings.gradle.kts`).
Asset game thật nằm trong `core/src/main/resources/game/` và được đóng gói tự động.

## PC (Desktop)

```bash
cd clients
./gradlew :desktop:run                 # chạy ngay
./gradlew :desktop:fatJar              # đóng gói chạy độc lập
java -jar desktop/build/libs/desktop-all.jar
```

## Android

Cần Android SDK (đặt `ANDROID_HOME` hoặc `local.properties`).

```bash
cd clients
./gradlew :android:assembleDebug
# APK: android/build/outputs/apk/debug/android-debug.apk
```

## iOS (Flutter — cũng chạy được Android)

```bash
cd clients/ios
flutter pub get
flutter run            # simulator/thiết bị
flutter build ipa      # bản iOS phát hành
flutter build apk      # bản Android phát hành
```

## J2ME (legacy)

Cần Ant + WTK 2.5.

```bash
cd clients/j2me
ant build              # tạo dist/vqsv.jar
```

## Cấu hình địa chỉ server

- **Desktop**: sửa host trong `desktop/src/main/kotlin/com/vqsv/desktop/DesktopLauncher.kt`
  (`VqsvGame("localhost")`) — đổi `localhost` thành IP/tên miền server.
- **Android**: tương tự trong `android/src/main/kotlin/com/vqsv/android/MainActivity.kt`.
- **iOS (Flutter)**: trong `ios/lib/services/` (api_service / tcp_service).
- **J2ME**: trong `j2me/src/com/vqsv/j2me/screen/LoginScreen.java`.

## Cập nhật asset từ game gốc

Asset đã được bake sẵn vào `core/src/main/resources/game/`. Chỉ chạy lại khi có
JAR game mới (bạn tự cung cấp):

```bash
python3 ../tools/asset-extractor/extract.py path/to/game.jar core/src/main/resources/game
```
