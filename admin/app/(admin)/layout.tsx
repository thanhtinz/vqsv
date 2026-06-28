"use client";

import Sidebar from "@/components/Sidebar";
import Topbar from "@/components/Topbar";
import Loading from "@/components/Loading";
import { useAdminGuard } from "@/lib/auth";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { ready, account } = useAdminGuard();

  if (!ready) {
    return (
      <div className="flex h-screen items-center justify-center bg-surface">
        <Loading label="Đang kiểm tra quyền truy cập..." />
      </div>
    );
  }

  return (
    <div className="flex h-screen overflow-hidden bg-surface">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar account={account} />
        <main className="flex-1 overflow-y-auto p-6">{children}</main>
      </div>
    </div>
  );
}
