# VQSV Web — Vương Quốc Sủng Vật

Website chính thức dành cho người chơi game **Vương Quốc Sủng Vật (VQSV)**.
Xây dựng bằng **Next.js 14 (App Router)** + **TypeScript** + **Tailwind CSS**.

## Tính năng

- Trang chủ: hero, tải game (Android / PC / iOS / J2ME), tin tức, sự kiện, trạng thái máy chủ, gói nạp nổi bật.
- Đăng nhập / Đăng ký (JWT, lưu token trong `localStorage`).
- Trang tài khoản `/tai-khoan` (bảo vệ): thông tin tài khoản, danh sách nhân vật, đổi mật khẩu, lịch sử giao dịch.
- Đổi giftcode `/giftcode` (bảo vệ).
- Nạp Xu `/nap`: chọn gói, tạo đơn, hiển thị liên kết thanh toán (mock).
- Webshop `/webshop`: mua vật phẩm bằng Xu, gửi tới nhân vật.
- Bảng xếp hạng `/bxh`: lọc theo máy chủ + chế độ liên server.
- Sự kiện `/su-kien`.
- Tin tức `/tin-tuc` và chi tiết `/tin-tuc/[slug]`.

## Yêu cầu

- Node.js 18.17+ (khuyến nghị 20+)
- Một backend REST đang chạy (mặc định `http://localhost:8080`).

## Cấu hình

Sao chép file môi trường mẫu và chỉnh nếu cần:

```bash
cp .env.example .env.local
```

| Biến                   | Mặc định                | Mô tả                          |
| ---------------------- | ----------------------- | ------------------------------ |
| `NEXT_PUBLIC_API_BASE` | `http://localhost:8080` | URL gốc của backend REST API. |

## Chạy dự án

```bash
npm install
npm run dev
```

Mở http://localhost:3000

## Build production

```bash
npm run build
npm run start
```

## Cấu trúc thư mục

```
app/
  layout.tsx            # Layout chung (Navbar + Footer + metadata)
  globals.css           # Tailwind + style toàn cục
  page.tsx              # Trang chủ
  login/ register/      # Xác thực
  tai-khoan/            # Hồ sơ tài khoản (bảo vệ)
  giftcode/             # Đổi giftcode (bảo vệ)
  nap/                  # Nạp Xu
  webshop/             # Webshop
  bxh/                  # Bảng xếp hạng
  su-kien/             # Sự kiện
  tin-tuc/             # Tin tức + [slug] chi tiết
components/             # Navbar, Footer, Card, Button, Input, Loading, AuthGuard
lib/
  api.ts               # apiGet / apiPost (đọc NEXT_PUBLIC_API_BASE, gắn Bearer token)
  auth.ts              # getToken/setToken/clearToken/getAccount + hook useAuth
  format.ts            # Định dạng tiền tệ / Xu / ngày giờ (vi-VN)
  types.ts             # Kiểu dữ liệu API
```

## Ghi chú về xác thực

- Sau khi đăng nhập/đăng ký thành công, `token` và `account` được lưu trong `localStorage`.
- Các request cần xác thực gửi kèm header `Authorization: Bearer <token>`.
- Các trang bảo vệ dùng `components/AuthGuard.tsx`; nếu chưa đăng nhập sẽ chuyển về `/login`.

## Ghi chú về thanh toán

Trang `/nap` gọi `POST /api/web/topup/order` rồi hiển thị `payUrl` trả về.
Trong môi trường thật, người dùng sẽ được chuyển hướng tới cổng thanh toán
(MoMo, ZaloPay, ...) để hoàn tất giao dịch. Ở đây chỉ hiển thị liên kết mô phỏng.
