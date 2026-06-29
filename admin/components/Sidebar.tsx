"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

interface NavItem {
  href: string;
  label: string;
}

interface NavGroup {
  title: string;
  items: NavItem[];
}

const groups: NavGroup[] = [
  {
    title: "Quản trị",
    items: [
      { href: "/", label: "Tổng quan" },
      { href: "/users", label: "Người dùng" },
      { href: "/payments", label: "Thanh toán" },
      { href: "/servers", label: "Máy chủ" },
      { href: "/audit", label: "Nhật ký" },
    ],
  },
  {
    title: "Vận hành",
    items: [
      { href: "/giftcodes", label: "Giftcode" },
      { href: "/topup-packages", label: "Gói nạp" },
      { href: "/payment-settings", label: "Cấu hình thanh toán" },
      { href: "/webshop", label: "Webshop" },
      { href: "/grant", label: "Tặng quà" },
    ],
  },
  {
    title: "Nội dung",
    items: [
      { href: "/news", label: "Tin tức" },
      { href: "/events", label: "Sự kiện" },
    ],
  },
  {
    title: "Nội dung game",
    items: [
      { href: "/game/maps", label: "Bản đồ" },
      { href: "/game/pets", label: "Sủng vật (mẫu)" },
      { href: "/game/items", label: "Vật phẩm" },
      { href: "/game/wild-pets", label: "Sủng vật hoang" },
      { href: "/game/shop", label: "Cửa hàng game" },
      { href: "/game/npcs", label: "NPC" },
      { href: "/game/enemies", label: "Quái" },
      { href: "/game/warps", label: "Cổng dịch chuyển" },
    ],
  },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="flex h-screen w-60 shrink-0 flex-col border-r border-surface-border bg-surface-card">
      <div className="flex items-center gap-2 border-b border-surface-border px-5 py-4">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand text-sm font-bold text-white">
          V
        </div>
        <div>
          <div className="text-sm font-bold leading-tight text-white">VQSV</div>
          <div className="text-[10px] text-gray-400">Bảng quản trị</div>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-3">
        {groups.map((group) => (
          <div key={group.title} className="mb-4">
            <div className="px-2 pb-1 text-[10px] font-semibold uppercase tracking-wider text-gray-500">
              {group.title}
            </div>
            <ul className="space-y-0.5">
              {group.items.map((item) => {
                const active =
                  item.href === "/"
                    ? pathname === "/"
                    : pathname.startsWith(item.href);
                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      className={`block rounded-md px-3 py-1.5 text-sm transition-colors ${
                        active
                          ? "bg-brand text-white"
                          : "text-gray-300 hover:bg-surface-hover hover:text-white"
                      }`}
                    >
                      {item.label}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </nav>

      <div className="border-t border-surface-border px-5 py-3 text-[10px] text-gray-600">
        Vương Quốc Sủng Vật &copy; {new Date().getFullYear()}
      </div>
    </aside>
  );
}
