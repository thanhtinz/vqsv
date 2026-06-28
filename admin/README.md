# VQSV Admin — Bảng quản trị Vương Quốc Sủng Vật

Bảng quản trị (admin panel) cho game **Vương Quốc Sủng Vật (VQSV)**, xây dựng bằng
**Next.js 14 (App Router) + TypeScript + Tailwind CSS**.

Giao diện tối, chuyên nghiệp: sidebar trái + thanh trên + nội dung chính với bảng dữ liệu,
form modal/inline. Toàn bộ văn bản tiếng Việt.

## Yêu cầu

- Node.js 18+ (khuyến nghị 20+)
- Backend VQSV chạy và cung cấp các API `/api/admin/*` (mặc định `http://localhost:8080`)

## Cài đặt & chạy

```bash
cd admin
cp .env.example .env.local   # chỉnh NEXT_PUBLIC_API_BASE nếu cần
npm install
npm run dev
```

Ứng dụng chạy ở cổng **3001** (`next dev -p 3001`) để tránh trùng với client game.
Mở http://localhost:3001

> Có thể đổi cổng trong `package.json` (script `dev`/`start`).

## Biến môi trường

| Biến | Mặc định | Mô tả |
| --- | --- | --- |
| `NEXT_PUBLIC_API_BASE` | `http://localhost:8080` | URL gốc của backend |

## Đăng nhập

- Dùng tài khoản in-game có vai trò **ADMIN** hoặc **GM**.
- `POST /api/web/auth/login {username,password}` trả về `{token, account}`.
- Token (JWT) được lưu trong `localStorage`, gửi kèm `Authorization: Bearer <token>` ở mọi request admin.
- Nếu `account.role` không phải `ADMIN`/`GM` sẽ báo "không có quyền".

## Cấu trúc

```
app/
  layout.tsx              # shell gốc + globals
  login/page.tsx          # trang đăng nhập
  (admin)/                # nhóm route được bảo vệ bởi guard admin
    layout.tsx            # sidebar + topbar + useAdminGuard
    page.tsx              # Tổng quan (dashboard)
    users/                # Người dùng (tìm kiếm, phân trang, sửa, mật khẩu, nhân vật)
    payments/             # Thanh toán (lọc, duyệt/từ chối)
    servers/              # Máy chủ (CRUD + gộp server + liên server)
    giftcodes/            # Giftcode
    topup-packages/       # Gói nạp
    webshop/              # Webshop
    news/                 # Tin tức
    events/               # Sự kiện
    grant/                # Tặng quà
    audit/                # Nhật ký
    game/                 # Nội dung game: maps, pets, items, wild-pets, shop, npcs, enemies, warps
lib/
  api.ts                  # apiGet/apiPost/apiPut/apiPatch/apiDelete (Bearer, ném {error})
  auth.ts                 # token+account localStorage, isAdmin, useAdminGuard
  format.ts               # định dạng VND/số/ngày
  reward.ts               # helper rewardJson
components/
  Sidebar, Topbar, DataTable, CrudResource, Modal, FormField,
  Button, StatCard, Loading, Alert, PageHeader
```

## Thành phần tái sử dụng

- **`DataTable`** — bảng generic điều khiển bằng cấu hình cột.
- **`CrudResource`** — component CRUD generic (danh sách + form modal tạo/sửa + xoá) điều khiển bằng
  `columns` + `fields`. Hầu hết các trang chỉ cần khai báo cấu hình, giúp code rất ngắn gọn.
  - `editable`: bật nút sửa (PUT `/{id}`).
  - `upsertViaPost`: cập nhật bằng POST (body chứa `id`) — dùng cho cửa hàng game.
  - `transformIn` / `transformOut`: biến đổi dữ liệu vào/ra (vd parse `rewardJson`, ánh xạ `item.id`).

## Định dạng rewardJson

Các trường thưởng dùng JSON dạng:

```json
{ "xu": 1000, "gold": 50000, "items": [{ "itemId": 1, "qty": 10 }] }
```

## Ghi chú

- Tất cả trang là client component và được bảo vệ bởi `useAdminGuard` (chuyển về `/login`
  nếu thiếu token hoặc không đủ quyền).
- Lỗi 401 từ API sẽ tự xoá phiên và đưa về trang đăng nhập.
