"use client";

import { useEffect, useState, type FormEvent } from "react";
import AuthGuard from "@/components/AuthGuard";
import Card, { SectionTitle } from "@/components/Card";
import Button from "@/components/Button";
import { Input } from "@/components/Input";
import { Loading, ErrorMessage, EmptyState } from "@/components/Loading";
import { apiGet, apiPost, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import {
  formatXu,
  formatVnd,
  formatNumber,
  formatDateTime,
} from "@/lib/format";
import type {
  Account,
  CharacterItem,
  TransactionItem,
} from "@/lib/types";

type Tab = "characters" | "password" | "transactions";

function statusBadge(status: string) {
  const s = status?.toUpperCase();
  if (s === "SUCCESS" || s === "DONE" || s === "COMPLETED")
    return "bg-emerald-500/15 text-emerald-400";
  if (s === "PENDING" || s === "PROCESSING")
    return "bg-amber-500/15 text-amber-400";
  if (s === "FAILED" || s === "CANCELLED")
    return "bg-red-500/15 text-red-400";
  return "bg-white/10 text-white/60";
}

function ProfileInner() {
  const { account, refreshAccount } = useAuth();
  const [tab, setTab] = useState<Tab>("characters");

  // Refresh authoritative account from the server on mount.
  useEffect(() => {
    apiGet<Account>("/api/web/profile", { auth: true })
      .then((a) => refreshAccount(a))
      .catch(() => {
        /* keep cached account */
      });
  }, [refreshAccount]);

  return (
    <div className="mx-auto max-w-5xl px-4 py-12">
      <h1 className="text-3xl font-black text-white">Tài khoản của tôi</h1>

      {/* Account summary */}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <p className="text-xs uppercase text-white/40">Tài khoản</p>
          <p className="mt-1 text-lg font-bold text-white">
            {account?.username}
          </p>
          <p className="text-xs text-white/50">{account?.email || "—"}</p>
        </Card>
        <Card>
          <p className="text-xs uppercase text-white/40">Số dư Xu</p>
          <p className="mt-1 text-lg font-bold text-brand">
            {formatXu(account?.balanceXu ?? 0)}
          </p>
        </Card>
        <Card>
          <p className="text-xs uppercase text-white/40">Tổng nạp</p>
          <p className="mt-1 text-lg font-bold text-white">
            {formatVnd(account?.totalTopup ?? 0)}
          </p>
        </Card>
        <Card>
          <p className="text-xs uppercase text-white/40">Trạng thái</p>
          <p className="mt-1 text-lg font-bold text-white">
            {account?.status || "—"}
          </p>
          <p className="text-xs text-white/50">Quyền: {account?.role}</p>
        </Card>
      </div>

      {/* Tabs */}
      <div className="mt-8 flex flex-wrap gap-2 border-b border-white/10">
        {(
          [
            ["characters", "Nhân vật"],
            ["transactions", "Lịch sử giao dịch"],
            ["password", "Đổi mật khẩu"],
          ] as [Tab, string][]
        ).map(([key, label]) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`-mb-px border-b-2 px-4 py-2.5 text-sm font-semibold transition-colors ${
              tab === key
                ? "border-brand text-brand"
                : "border-transparent text-white/60 hover:text-white"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {tab === "characters" && <CharactersTab />}
        {tab === "transactions" && <TransactionsTab />}
        {tab === "password" && <PasswordTab />}
      </div>
    </div>
  );
}

function CharactersTab() {
  const [chars, setChars] = useState<CharacterItem[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    apiGet<CharacterItem[]>("/api/web/profile/characters", { auth: true })
      .then(setChars)
      .catch((e) =>
        setError(e instanceof ApiError ? e.message : "Không tải được nhân vật.")
      );
  }, []);

  if (error) return <ErrorMessage message={error} />;
  if (!chars) return <Loading label="Đang tải nhân vật..." />;
  if (chars.length === 0)
    return <EmptyState message="Bạn chưa có nhân vật nào." />;

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {chars.map((c) => (
        <Card key={c.id} hover>
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-white">{c.name}</h3>
            <span className="rounded-full bg-brand/15 px-2 py-0.5 text-xs font-semibold text-brand">
              Lv.{c.level}
            </span>
          </div>
          <p className="mt-1 text-xs text-white/50">{c.serverName}</p>
          <div className="mt-3 flex justify-between text-sm">
            <span className="text-white/60">
              Kim tiền:{" "}
              <span className="text-white">{formatNumber(c.kimTien)}</span>
            </span>
            <span className="text-white/60">
              Huy chương:{" "}
              <span className="text-white">{formatNumber(c.huyChuong)}</span>
            </span>
          </div>
        </Card>
      ))}
    </div>
  );
}

function TransactionsTab() {
  const [items, setItems] = useState<TransactionItem[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    apiGet<TransactionItem[]>("/api/web/profile/transactions", { auth: true })
      .then(setItems)
      .catch((e) =>
        setError(
          e instanceof ApiError ? e.message : "Không tải được giao dịch."
        )
      );
  }, []);

  if (error) return <ErrorMessage message={error} />;
  if (!items) return <Loading label="Đang tải giao dịch..." />;
  if (items.length === 0)
    return <EmptyState message="Chưa có giao dịch nào." />;

  return (
    <Card className="overflow-x-auto p-0">
      <table className="w-full min-w-[640px] text-left text-sm">
        <thead className="border-b border-white/10 text-white/50">
          <tr>
            <th className="px-4 py-3 font-medium">Thời gian</th>
            <th className="px-4 py-3 font-medium">Loại</th>
            <th className="px-4 py-3 font-medium">Số tiền</th>
            <th className="px-4 py-3 font-medium">Xu</th>
            <th className="px-4 py-3 font-medium">Kênh</th>
            <th className="px-4 py-3 font-medium">Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          {items.map((t) => (
            <tr key={t.id} className="border-b border-white/5 last:border-0">
              <td className="px-4 py-3 text-white/70">
                {formatDateTime(t.createdAt)}
              </td>
              <td className="px-4 py-3 text-white">{t.kind}</td>
              <td className="px-4 py-3 text-white/80">
                {t.amountVnd ? formatVnd(t.amountVnd) : "—"}
              </td>
              <td className="px-4 py-3 text-brand">
                {t.xu ? formatNumber(t.xu) : "—"}
              </td>
              <td className="px-4 py-3 text-white/60">{t.provider || "—"}</td>
              <td className="px-4 py-3">
                <span
                  className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusBadge(
                    t.status
                  )}`}
                >
                  {t.status}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  );
}

function PasswordTab() {
  const [oldPassword, setOld] = useState("");
  const [newPassword, setNew] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setSuccess("");
    if (newPassword !== confirm) {
      setError("Mật khẩu mới xác nhận không khớp.");
      return;
    }
    if (newPassword.length < 6) {
      setError("Mật khẩu mới phải có ít nhất 6 ký tự.");
      return;
    }
    setLoading(true);
    try {
      await apiPost("/api/web/profile/password", {
        oldPassword,
        newPassword,
      }, { auth: true });
      setSuccess("Đổi mật khẩu thành công.");
      setOld("");
      setNew("");
      setConfirm("");
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : "Đổi mật khẩu thất bại."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card className="max-w-md">
      <SectionTitle title="Đổi mật khẩu" />
      <form onSubmit={onSubmit} className="flex flex-col gap-4">
        {error && <ErrorMessage message={error} />}
        {success && (
          <div className="rounded-lg border border-emerald-500/40 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-300">
            {success}
          </div>
        )}
        <Input
          id="old"
          label="Mật khẩu hiện tại"
          type="password"
          autoComplete="current-password"
          value={oldPassword}
          onChange={(e) => setOld(e.target.value)}
          required
        />
        <Input
          id="new"
          label="Mật khẩu mới"
          type="password"
          autoComplete="new-password"
          value={newPassword}
          onChange={(e) => setNew(e.target.value)}
          required
        />
        <Input
          id="confirm"
          label="Xác nhận mật khẩu mới"
          type="password"
          autoComplete="new-password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          required
        />
        <Button type="submit" disabled={loading} className="mt-1">
          {loading ? "Đang xử lý..." : "Cập nhật mật khẩu"}
        </Button>
      </form>
    </Card>
  );
}

export default function ProfilePage() {
  return (
    <AuthGuard>
      <ProfileInner />
    </AuthGuard>
  );
}
