# Vương Quốc Sủng Vật Online (VQSV)

> Game nhập vai thu phục thú cưng huyền thoại trên điện thoại Java (J2ME),
> nay được dựng lại thành **game online nhiều người chơi** — giữ nguyên thế giới,
> sủng vật và lối chơi gốc, thêm bạn bè cùng online trên một thế giới chung.

---

## Giới thiệu

Ngày xưa, **Vương Quốc Sủng Vật** là tựa game offline trên điện thoại "cục gạch":
bạn lang thang qua các vùng đất, chạm trán sủng vật hoang dã, thu phục chúng bằng
những chiếc Tất, nuôi lớn rồi tiến hóa, và thử sức với các huấn luyện viên.

VQSV Online tái dựng trọn vẹn trải nghiệm đó cho thời đại mới:

- 🐾 **Thu phục & nuôi sủng vật** — bắt thú hoang, lên cấp, tiến hóa thành dạng mạnh hơn.
- ⚔️ **Chiến đấu theo lượt** — hệ tương khắc nguyên tố, chí mạng, chiến thuật đổi sủng vật.
- 🗺️ **Phiêu lưu qua 6 vùng đất** — từ Làng Khởi Đầu yên bình đến Tháp Bóng Tối hiểm nguy.
- 👥 **Thế giới online chung** — thấy người chơi khác di chuyển theo thời gian thực, trò chuyện.
- 🥊 **Đấu PvP & đấu Huấn Luyện Viên** — thách đấu bạn bè, hoặc đến gặp NPC trainer để giao đấu.
- 🏪 **Cửa hàng & vật phẩm** — Tất bắt thú, thuốc hồi máu, kẹo tăng cấp, gói kim tiền…

---

## Thế giới game

### Sủng vật
Hàng loạt loài sủng vật thuộc nhiều **nguyên tố** khác nhau (Lửa, Nước, Gió, Đất,
Sáng, Tối…), mỗi loài có chỉ số HP / Tấn công / Phòng thủ / Tốc độ riêng và đường
tiến hóa riêng. Nuôi đủ cấp, sủng vật sẽ tiến hóa thành dạng trưởng thành mạnh mẽ hơn.

### Chiến đấu
- **Theo lượt**, tốc độ (SPD) quyết định ai ra đòn trước.
- **Tương khắc nguyên tố** tạo lợi thế/bất lợi sát thương.
- **Chí mạng** dựa trên tốc độ của sủng vật.
- Trong trận có thể: **Tấn công**, **Dùng vật phẩm**, **Bắt** (với thú hoang), **Chạy trốn**.

### Bản đồ & phiêu lưu
6 vùng đất nối tiếp theo độ khó tăng dần. Đi bộ trên bản đồ có thể gặp **thú hoang ngẫu nhiên**.
Người chơi khác hiện diện ngay trên cùng bản đồ — bạn thấy họ di chuyển trong thời gian thực.

### Huấn luyện viên (Trainer)
Rải rác trên các bản đồ là những **NPC huấn luyện viên**. Đi tới đứng cạnh họ để
khiêu chiến — thắng được thưởng kinh nghiệm và kim tiền, đúng kiểu các trận "đấu NPC"
của bản gốc.

### PvP
Gặp người chơi khác trên bản đồ, bạn có thể **thách đấu** trực tiếp. Đối phương nhận
lời mời và đồng ý là trận đấu PvP theo lượt bắt đầu.

### Tiền tệ & cửa hàng
- **Kim Tiền** — kiếm từ chiến đấu, dùng mua vật phẩm phổ thông.
- **Huy Chương** — dùng cho vật phẩm hiếm.

Bản gốc dùng SMS để mua vật phẩm; VQSV Online thay bằng hệ thống tài khoản miễn phí
và cửa hàng trong game:

| Cơ chế trả phí bản gốc | Thay thế trong VQSV Online |
|------------------------|----------------------------|
| Kích hoạt bằng SMS     | Đăng ký tài khoản miễn phí |
| Mua Tất trúng cầu      | Mua bằng Huy Chương trong shop |
| Mua kim tiền           | Gói Kim Tiền trong shop |
| Mua lượt tăng cấp      | Kẹo Tăng Cấp |
| Mở huy hiệu            | Chiến đấu để mở khóa |

---

## Chơi ở đâu

VQSV Online chạy trên nhiều nền tảng, tất cả cùng kết nối về một thế giới chung:

| Nền tảng | Mô tả |
|----------|-------|
| 📱 **Android** | Bản dựng LibGDX gốc |
| 💻 **PC (Windows/Mac/Linux)** | Bản desktop LibGDX |
| 🍎 **iOS** | Bản Flutter |
| ☎️ **J2ME** | Bản điện thoại Java đời cũ (legacy) |

---

## Dành cho lập trình viên

Đây là dự án phục dựng (reverse-engineer) từ bản J2ME gốc thành kiến trúc client–server online.

- **Hướng dẫn triển khai server lên VPS:** [`deploy/VPS_DEPLOY.md`](deploy/VPS_DEPLOY.md)
- **Tài liệu kỹ thuật:**
  - Định dạng asset gốc — [`docs/ASSET-FORMATS.md`](docs/ASSET-FORMATS.md)
  - Dữ liệu game gốc — [`docs/ORIGINAL-DATA.md`](docs/ORIGINAL-DATA.md)
  - Giao thức gốc — [`docs/ORIGINAL-PROTOCOL.md`](docs/ORIGINAL-PROTOCOL.md)
  - Cơ chế gameplay gốc — [`docs/ORIGINAL-MECHANICS.md`](docs/ORIGINAL-MECHANICS.md)
- **Cấu trúc thư mục:**

```
vqsv/
├── server/     # Server Spring Boot + Kotlin (REST, WebSocket, TCP gateway, PostgreSQL, Redis)
├── clients/    # Client game
│   ├── core/      # Code dùng chung (LibGDX/Kotlin): net, model, asset, screen
│   ├── android/   # Wrapper Android
│   ├── desktop/   # Wrapper PC (LWJGL3)
│   ├── ios/       # Bản Flutter
│   └── j2me/      # Bản J2ME gốc
├── web/        # Website người chơi (Next.js)
├── admin/      # Trang quản trị (Next.js)
├── tools/      # Công cụ trích xuất asset từ JAR gốc
└── docs/       # Tài liệu kỹ thuật
```

> 🎮 *Một dự án tâm huyết hồi sinh tuổi thơ — đưa Vương Quốc Sủng Vật trở lại,
> lần này cùng tất cả mọi người.*
