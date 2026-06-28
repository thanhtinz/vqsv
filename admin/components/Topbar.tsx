"use client";

import { useRouter } from "next/navigation";
import { Account, clearAuth } from "@/lib/auth";
import Button from "./Button";

export default function Topbar({ account }: { account: Account | null }) {
  const router = useRouter();

  function logout() {
    clearAuth();
    router.replace("/login");
  }

  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-surface-border bg-surface-card px-6">
      <div className="text-sm text-gray-400">
        Quản trị hệ thống Vương Quốc Sủng Vật
      </div>
      <div className="flex items-center gap-3">
        {account && (
          <div className="text-right">
            <div className="text-sm font-medium text-white">
              {account.username}
            </div>
            <div className="text-[11px] text-brand-light">{account.role}</div>
          </div>
        )}
        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand text-sm font-bold uppercase text-white">
          {account?.username?.charAt(0) || "A"}
        </div>
        <Button variant="secondary" size="sm" onClick={logout}>
          Đăng xuất
        </Button>
      </div>
    </header>
  );
}
