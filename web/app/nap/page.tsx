"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Card, { SectionTitle } from "@/components/Card";
import Button from "@/components/Button";
import { Loading, ErrorMessage, EmptyState } from "@/components/Loading";
import { apiGet, apiPost, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { formatVnd, formatXu } from "@/lib/format";
import type { TopupPackage, TopupOrderResult } from "@/lib/types";

function Row({
  label,
  value,
  mono,
  highlight,
}: {
  label: string;
  value?: string | null;
  mono?: boolean;
  highlight?: boolean;
}) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-white/50">{label}</span>
      <span
        className={`${mono ? "font-mono" : ""} ${
          highlight ? "font-bold text-brand-light" : "text-white"
        }`}
      >
        {value || "—"}
      </span>
    </div>
  );
}

export default function TopupPage() {
  const { account, isAuthenticated, ready } = useAuth();
  const [packages, setPackages] = useState<TopupPackage[] | null>(null);
  const [loadError, setLoadError] = useState("");

  const [pendingId, setPendingId] = useState<number | null>(null);
  const [orderError, setOrderError] = useState("");
  const [order, setOrder] = useState<TopupOrderResult | null>(null);

  useEffect(() => {
    apiGet<TopupPackage[]>("/api/web/public/topup/packages")
      .then(setPackages)
      .catch((e) =>
        setLoadError(
          e instanceof ApiError ? e.message : "Không tải được gói nạp."
        )
      );
  }, []);

  async function handleOrder(pkg: TopupPackage) {
    setOrderError("");
    setOrder(null);
    setPendingId(pkg.id);
    try {
      const res = await apiPost<TopupOrderResult>(
        "/api/web/topup/order",
        { packageId: pkg.id },
        { auth: true }
      );
      setOrder(res);
    } catch (err) {
      setOrderError(
        err instanceof ApiError ? err.message : "Tạo đơn nạp thất bại."
      );
    } finally {
      setPendingId(null);
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-12">
      <SectionTitle
        title="Nạp Xu"
        subtitle="Chọn gói nạp và phương thức thanh toán phù hợp."
      />

      {ready && isAuthenticated && account && (
        <Card className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs uppercase text-white/40">
              Số dư hiện tại
            </p>
            <p className="text-xl font-black text-brand">
              {formatXu(account.balanceXu)}
            </p>
          </div>
          <div className="text-right text-xs text-white/50">
            Thanh toán qua chuyển khoản ngân hàng
            <br />
            (quét QR — cộng xu tự động)
          </div>
        </Card>
      )}

      {ready && !isAuthenticated && (
        <Card className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-white/70">
            Vui lòng đăng nhập để nạp Xu vào tài khoản.
          </p>
          <Button href="/login?redirect=/nap" size="sm">
            Đăng nhập
          </Button>
        </Card>
      )}

      {orderError && (
        <div className="mb-6">
          <ErrorMessage message={orderError} />
        </div>
      )}

      {order && (
        <Card className="mb-6 border-brand/40">
          <h3 className="text-lg font-bold text-white">Đơn nạp đã tạo</h3>
          <div className="mt-2 grid gap-1 text-sm text-white/70">
            <p>
              Mã giao dịch:{" "}
              <span className="font-mono text-white">
                {order.transactionId}
              </span>
            </p>
            <p>
              Số tiền:{" "}
              <span className="text-white">{formatVnd(order.amountVnd)}</span>
            </p>
            <p>
              Trạng thái: <span className="text-amber-400">{order.status}</span>
            </p>
          </div>
          {order.provider === "SEPAY" ? (
            <div className="mt-4">
              <p className="text-sm text-white/70">
                Quét mã QR hoặc chuyển khoản theo đúng thông tin bên dưới. Xu sẽ
                được cộng <strong>tự động</strong> sau khi nhận được tiền.
              </p>
              <div className="mt-4 flex flex-col gap-4 sm:flex-row sm:items-start">
                {order.qrUrl && (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={order.qrUrl}
                    alt="QR chuyển khoản"
                    className="h-48 w-48 rounded-lg bg-white p-2"
                  />
                )}
                <div className="grid gap-1.5 text-sm">
                  <Row label="Ngân hàng" value={order.bankCode} />
                  <Row label="Số tài khoản" value={order.bankAccount} mono />
                  <Row label="Chủ tài khoản" value={order.accountHolder} />
                  <Row label="Số tiền" value={formatVnd(order.amountVnd)} />
                  <Row label="Nội dung CK" value={order.transferContent} mono highlight />
                </div>
              </div>
              <p className="mt-3 text-xs text-amber-400/90">
                ⚠️ Phải ghi đúng nội dung chuyển khoản{" "}
                <span className="font-mono">{order.transferContent}</span> để hệ
                thống tự cộng xu.
              </p>
            </div>
          ) : (
            <>
              <p className="mt-3 text-sm text-white/60">
                Chuyển khoản thủ công theo hướng dẫn; admin sẽ duyệt và cộng xu.
              </p>
              <div className="mt-3">
                <Button href={order.payUrl || "#"} external>
                  Tới trang thanh toán →
                </Button>
              </div>
            </>
          )}
        </Card>
      )}

      {loadError ? (
        <ErrorMessage message={loadError} />
      ) : !packages ? (
        <Loading label="Đang tải gói nạp..." />
      ) : packages.length === 0 ? (
        <EmptyState message="Chưa có gói nạp nào." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {packages.map((p) => (
            <Card key={p.id} hover className="flex flex-col text-center">
              <h3 className="font-bold text-white">{p.name}</h3>
              <p className="mt-3 text-2xl font-black text-brand">
                {formatXu(p.totalXu)}
              </p>
              <p className="text-xs text-white/50">
                {formatXu(p.xuAmount)}
                {p.bonusXu > 0 && (
                  <span className="text-emerald-400">
                    {" "}
                    + {formatXu(p.bonusXu)} thưởng
                  </span>
                )}
              </p>
              <p className="mt-2 text-lg font-semibold text-white">
                {formatVnd(p.priceVnd)}
              </p>
              <div className="mt-auto pt-4">
                {isAuthenticated ? (
                  <Button
                    onClick={() => handleOrder(p)}
                    disabled={pendingId === p.id}
                    className="w-full"
                  >
                    {pendingId === p.id ? "Đang tạo đơn..." : "Nạp"}
                  </Button>
                ) : (
                  <Link
                    href="/login?redirect=/nap"
                    className="block w-full rounded-lg border border-white/10 px-4 py-2.5 text-sm font-semibold text-white/70 hover:border-brand/60 hover:text-brand"
                  >
                    Đăng nhập để nạp
                  </Link>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
