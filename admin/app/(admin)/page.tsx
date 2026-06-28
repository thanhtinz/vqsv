"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { apiGet } from "@/lib/api";
import StatCard from "@/components/StatCard";
import Loading from "@/components/Loading";
import Alert from "@/components/Alert";
import PageHeader from "@/components/PageHeader";
import { formatNumber, formatVnd } from "@/lib/format";

interface DashboardStats {
  totalAccounts: number;
  totalCharacters: number;
  totalRevenueVnd: number;
  pendingPayments: number;
  serverCount: number;
  adminCount: number;
}

const quickLinks = [
  { href: "/users", label: "Người dùng" },
  { href: "/payments", label: "Thanh toán" },
  { href: "/servers", label: "Máy chủ" },
  { href: "/giftcodes", label: "Giftcode" },
  { href: "/grant", label: "Tặng quà" },
  { href: "/news", label: "Tin tức" },
];

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const data = await apiGet<DashboardStats>("/api/admin/dashboard");
        setStats(data);
      } catch (e: any) {
        setError(e.message || "Không tải được số liệu");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  return (
    <div>
      <PageHeader
        title="Tổng quan"
        description="Số liệu nhanh toàn hệ thống"
      />

      {error && <Alert kind="error">{error}</Alert>}

      {loading ? (
        <Loading />
      ) : stats ? (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <StatCard
              label="Tổng tài khoản"
              value={formatNumber(stats.totalAccounts)}
            />
            <StatCard
              label="Tổng nhân vật"
              value={formatNumber(stats.totalCharacters)}
              accent="text-sky-400"
            />
            <StatCard
              label="Doanh thu"
              value={formatVnd(stats.totalRevenueVnd)}
              accent="text-emerald-400"
            />
            <StatCard
              label="Giao dịch chờ duyệt"
              value={formatNumber(stats.pendingPayments)}
              accent="text-amber-400"
              hint="Cần xử lý"
            />
            <StatCard
              label="Số máy chủ"
              value={formatNumber(stats.serverCount)}
              accent="text-pink-400"
            />
            <StatCard
              label="Quản trị viên"
              value={formatNumber(stats.adminCount)}
              accent="text-violet-400"
            />
          </div>

          <h2 className="mb-3 mt-8 text-sm font-semibold uppercase tracking-wide text-gray-400">
            Truy cập nhanh
          </h2>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
            {quickLinks.map((l) => (
              <Link
                key={l.href}
                href={l.href}
                className="rounded-xl border border-surface-border bg-surface-card px-4 py-5 text-center text-sm font-medium text-gray-200 transition-colors hover:border-brand-light hover:bg-surface-hover"
              >
                {l.label}
              </Link>
            ))}
          </div>
        </>
      ) : null}
    </div>
  );
}
