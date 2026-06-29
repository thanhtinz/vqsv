# VQSV — Hướng dẫn triển khai lên VPS

Triển khai toàn bộ hệ thống trên một VPS Ubuntu 22.04/24.04 mới tinh trong khoảng
**10 phút**:
**server (Spring Boot) + PostgreSQL + Redis + website người chơi (Next.js) +
trang quản trị (Next.js) + Caddy làm reverse proxy HTTPS tự động.**

---

## 0. Yêu cầu

- Một **VPS có IP công khai** (khuyến nghị tối thiểu 2 vCPU / 4 GB RAM — vì hệ thống
  còn build thêm hai ứng dụng Next.js bên cạnh server Java).
- Một **tên miền** với hai bản ghi **A record** trỏ về IP của VPS:
  - `DOMAIN`        → website + API   (ví dụ: `play.yourgame.com` → `203.0.113.10`)
  - `admin.DOMAIN`  → trang quản trị  (ví dụ: `admin.play.yourgame.com` → cùng IP)

Sau khi chạy `docker compose -f docker-compose.prod.yml up -d --build`, hệ thống
sẽ phục vụ:

| URL | Dịch vụ |
|-----|---------|
| `https://DOMAIN/` | Website người chơi |
| `https://DOMAIN/api/*`, `/ws` | REST API + WebSocket |
| `https://admin.DOMAIN/` | Trang quản trị |
| `DOMAIN:9090` | Cổng TCP nhị phân cho J2ME |

---

## 1. Cài đặt Docker

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER && newgrp docker   # dùng docker không cần sudo
```

---

## 2. Lấy mã nguồn

```bash
sudo mkdir -p /opt/vqsv && sudo chown $USER /opt/vqsv
git clone https://github.com/thanhtinz/vqsv.git /opt/vqsv
cd /opt/vqsv
```

---

## 3. Cấu hình thông tin bí mật

```bash
cp .env.example .env
./scripts/gen-secrets.sh          # tự sinh mật khẩu DB/Redis và JWT secret
nano .env                         # đặt PUBLIC_DOMAIN và ACME_EMAIL
```

Trong file `.env`, cần khai báo tối thiểu:

- `PUBLIC_DOMAIN` — tên miền của bạn (ví dụ `play.yourgame.com`).
- `ACME_EMAIL` — email để Let's Encrypt gửi thông báo về chứng chỉ.

Các giá trị `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET` đã được
`gen-secrets.sh` điền tự động bằng chuỗi ngẫu nhiên mạnh. **Tuyệt đối không commit
file `.env` thật lên git** (đã được `.gitignore` loại trừ).

---

## 4. Mở tường lửa

```bash
sudo ufw allow 22/tcp     # SSH
sudo ufw allow 80/tcp     # HTTP (xác thực Let's Encrypt + chuyển hướng HTTPS)
sudo ufw allow 443/tcp    # HTTPS (REST API + WebSocket)
sudo ufw allow 9090/tcp   # Cổng TCP nhị phân cho J2ME
sudo ufw enable
```

---

## 5. Khởi chạy

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Lần chạy đầu tiên sẽ: build image của server, chạy migration Flyway cho cơ sở dữ liệu,
và Caddy tự động xin chứng chỉ TLS cho tên miền của bạn. Theo dõi tiến trình:

```bash
docker compose -f docker-compose.prod.yml logs -f server caddy
```

---

## 6. Kiểm tra

```bash
curl https://$PUBLIC_DOMAIN/actuator/health        # kỳ vọng: {"status":"UP"}

# Đăng ký một tài khoản thử nghiệm:
curl -X POST https://$PUBLIC_DOMAIN/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"test","password":"test1234","playerName":"Tester"}'
```

Các client sẽ kết nối tới:

| Client | Điểm kết nối |
|--------|--------------|
| Android / iOS / Web | `https://PUBLIC_DOMAIN` (REST) + `wss://PUBLIC_DOMAIN/ws` (STOMP) |
| J2ME (legacy) | `PUBLIC_DOMAIN:9090` (TCP thuần) |

---

## 7. Vận hành

```bash
# Cập nhật lên mã nguồn mới nhất:
git pull && docker compose -f docker-compose.prod.yml up -d --build

# Xem log:
docker compose -f docker-compose.prod.yml logs -f server

# Dừng / khởi động lại:
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d

# Sao lưu cơ sở dữ liệu (nên thêm vào cron — xem deploy/backup-db.sh):
./deploy/backup-db.sh
```

---

## Những gì bản triển khai này lo, và những gì còn lại thuộc phía vận hành

**Đã được lo sẵn trong bản triển khai này:** HTTPS tự động, lưu trữ và healthcheck
cho cơ sở dữ liệu, Redis có mật khẩu + bền vững dữ liệu (AOF), tự khởi động lại khi lỗi,
tắt máy an toàn (graceful shutdown), migration schema, ghi log, giới hạn tài nguyên,
và sao lưu cơ sở dữ liệu.

**Đã tích hợp sẵn cổng thanh toán SePay** (nạp qua chuyển khoản ngân hàng + QR, xu
cộng tự động qua webhook). **Toàn bộ cấu hình nằm trong trang Admin** — không có gì
trong code/`.env`:

1. Vào Admin ▸ **Cấu hình thanh toán**, điền: bật cổng, **SePay API Key**, **số tài
   khoản**, **mã ngân hàng (VietQR)**, tên chủ TK, tiền tố nội dung CK.
2. Trong bảng điều khiển SePay, khai báo **Webhook URL** trỏ tới:
   `https://PUBLIC_DOMAIN/api/web/public/topup/sepay/webhook` và đặt API Key trùng
   với key bạn vừa nhập ở Admin.

Khi tắt cổng SePay, web tự động dùng luồng nạp thủ công (admin duyệt).

**Vẫn là trách nhiệm của bạn trước khi thu phí:**

- **Tài khoản SePay** (sepay.vn) đã liên kết ngân hàng để lấy API Key thật; phần
  tích hợp kỹ thuật đã có sẵn.
- **Mở rộng nội dung game** — thêm bản đồ/sủng vật/vật phẩm ngoài dữ liệu seed ban đầu.
- **Quản trị & kiểm duyệt** — trang admin và chống gian lận cho server có thẩm quyền.
- **Pháp lý** — Điều khoản dịch vụ, chính sách bảo mật, và (tại Việt Nam) **giấy phép
  G1** để vận hành game online thương mại.
- **Giám sát & cảnh báo** — ví dụ Uptime Kuma kiểm tra `/actuator/health`, cùng việc
  lưu trữ bản sao lưu ở nơi khác (off-site).
