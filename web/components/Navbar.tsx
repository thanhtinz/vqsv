"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/lib/auth";
import { formatXu } from "@/lib/format";

const NAV_LINKS: { href: string; label: string }[] = [
  { href: "/tin-tuc", label: "Tin Tức" },
  { href: "/su-kien", label: "Sự Kiện" },
  { href: "/bxh", label: "Bảng Xếp Hạng" },
  { href: "/nap", label: "Nạp Thẻ" },
  { href: "/webshop", label: "Webshop" },
  { href: "/giftcode", label: "Giftcode" },
];

export function Navbar() {
  const { account, isAuthenticated, ready, logout } = useAuth();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  const isActive = (href: string) =>
    pathname === href || pathname.startsWith(href + "/");

  return (
    <header className="sticky top-0 z-50 border-b border-white/10 bg-bg/90 backdrop-blur">
      <nav className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4 px-4">
        <Link href="/" className="flex items-center gap-2">
          <span className="grid h-9 w-9 place-items-center rounded-lg bg-brand text-lg font-black text-bg shadow-glow">
            V
          </span>
          <span className="hidden text-lg font-extrabold tracking-wide text-white sm:block">
            VQSV
            <span className="ml-1 text-xs font-medium text-brand">
              Vương Quốc Sủng Vật
            </span>
          </span>
        </Link>

        <div className="hidden items-center gap-1 lg:flex">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                isActive(link.href)
                  ? "text-brand"
                  : "text-white/70 hover:text-white"
              }`}
            >
              {link.label}
            </Link>
          ))}
        </div>

        <div className="hidden items-center gap-3 lg:flex">
          {ready && isAuthenticated && account ? (
            <>
              <Link
                href="/tai-khoan"
                className="flex flex-col items-end leading-tight"
              >
                <span className="text-sm font-semibold text-white">
                  {account.username}
                </span>
                <span className="text-xs text-brand">
                  {formatXu(account.balanceXu)}
                </span>
              </Link>
              <button
                onClick={logout}
                className="rounded-lg border border-white/10 px-3 py-2 text-sm text-white/70 transition-colors hover:border-red-500/60 hover:text-red-400"
              >
                Đăng xuất
              </button>
            </>
          ) : (
            <>
              <Link
                href="/login"
                className="rounded-lg px-3 py-2 text-sm font-medium text-white/80 hover:text-white"
              >
                Đăng nhập
              </Link>
              <Link
                href="/register"
                className="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-bg hover:bg-brand-light"
              >
                Đăng ký
              </Link>
            </>
          )}
        </div>

        <button
          className="text-white/80 lg:hidden"
          onClick={() => setOpen((v) => !v)}
          aria-label="Mở menu"
        >
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
            <path
              d="M4 6h16M4 12h16M4 18h16"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
            />
          </svg>
        </button>
      </nav>

      {open && (
        <div className="border-t border-white/10 bg-bg-soft px-4 py-3 lg:hidden">
          <div className="flex flex-col gap-1">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setOpen(false)}
                className={`rounded-md px-3 py-2 text-sm font-medium ${
                  isActive(link.href)
                    ? "bg-bg-card text-brand"
                    : "text-white/70 hover:bg-bg-card hover:text-white"
                }`}
              >
                {link.label}
              </Link>
            ))}
            <div className="my-2 h-px bg-white/10" />
            {ready && isAuthenticated && account ? (
              <>
                <Link
                  href="/tai-khoan"
                  onClick={() => setOpen(false)}
                  className="rounded-md px-3 py-2 text-sm text-white"
                >
                  {account.username} ·{" "}
                  <span className="text-brand">
                    {formatXu(account.balanceXu)}
                  </span>
                </Link>
                <button
                  onClick={() => {
                    logout();
                    setOpen(false);
                  }}
                  className="rounded-md px-3 py-2 text-left text-sm text-red-400"
                >
                  Đăng xuất
                </button>
              </>
            ) : (
              <>
                <Link
                  href="/login"
                  onClick={() => setOpen(false)}
                  className="rounded-md px-3 py-2 text-sm text-white"
                >
                  Đăng nhập
                </Link>
                <Link
                  href="/register"
                  onClick={() => setOpen(false)}
                  className="rounded-md bg-brand px-3 py-2 text-sm font-semibold text-bg"
                >
                  Đăng ký
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}

export default Navbar;
