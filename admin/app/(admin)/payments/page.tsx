"use client";

import { useCallback, useEffect, useState } from "react";
import { apiGet, apiPost } from "@/lib/api";
import DataTable, { Column } from "@/components/DataTable";
import Button from "@/components/Button";
import Loading from "@/components/Loading";
import Alert from "@/components/Alert";
import PageHeader from "@/components/PageHeader";
import { Select } from "@/components/FormField";
import { formatDate, formatNumber, formatVnd } from "@/lib/format";

interface Payment {
  id: number;
  accountId: number;
  packageId: number;
  amountVnd: number;
  xuGranted: number;
  provider: string;
  providerRef: string;
  status: string;
  note: string;
  createdAt: string;
}

const PAGE_SIZE = 20;

export default function PaymentsPage() {
  const [rows, setRows] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await apiGet<Payment[]>(
        `/api/admin/payments?status=${encodeURIComponent(
          status
        )}&page=${page}&size=${PAGE_SIZE}`
      );
      setRows(Array.isArray(data) ? data : []);
    } catch (e: any) {
      setError(e.message || "Không tải được danh sách giao dịch");
    } finally {
      setLoading(false);
    }
  }, [status, page]);

  useEffect(() => {
    load();
  }, [load]);

  async function act(p: Payment, kind: "approve" | "reject") {
    if (
      !confirm(
        `${kind === "approve" ? "Duyệt" : "Từ chối"} giao dịch #${p.id}?`
      )
    )
      return;
    setBusyId(p.id);
    try {
      await apiPost(`/api/admin/payments/${p.id}/${kind}`);
      setNotice(
        `Đã ${kind === "approve" ? "duyệt" : "từ chối"} giao dịch #${p.id}`
      );
      await load();
    } catch (e: any) {
      setError(e.message || "Thao tác thất bại");
    } finally {
      setBusyId(null);
    }
  }

  const columns: Column<Payment>[] = [
    { key: "id", header: "ID", width: "60px" },
    { key: "accountId", header: "Tài khoản" },
    { key: "packageId", header: "Gói" },
    {
      key: "amountVnd",
      header: "Số tiền",
      render: (p) => formatVnd(p.amountVnd),
    },
    {
      key: "xuGranted",
      header: "Xu nhận",
      render: (p) => formatNumber(p.xuGranted),
    },
    { key: "provider", header: "Cổng" },
    { key: "providerRef", header: "Mã GD" },
    {
      key: "status",
      header: "Trạng thái",
      render: (p) => {
        const cls =
          p.status === "PENDING"
            ? "bg-amber-500/20 text-amber-300"
            : p.status === "APPROVED" || p.status === "SUCCESS"
            ? "bg-emerald-500/20 text-emerald-300"
            : "bg-red-500/20 text-red-300";
        return (
          <span className={`rounded px-2 py-0.5 text-xs ${cls}`}>
            {p.status}
          </span>
        );
      },
    },
    {
      key: "createdAt",
      header: "Thời gian",
      render: (p) => formatDate(p.createdAt),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Thanh toán"
        description="Duyệt hoặc từ chối các giao dịch nạp xu"
      />

      {error && (
        <Alert kind="error" onClose={() => setError("")}>
          {error}
        </Alert>
      )}
      {notice && (
        <Alert kind="success" onClose={() => setNotice("")}>
          {notice}
        </Alert>
      )}

      <div className="mb-4 flex items-center gap-2">
        <Select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
          className="max-w-[200px]"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="PENDING">PENDING</option>
          <option value="APPROVED">APPROVED</option>
          <option value="REJECTED">REJECTED</option>
        </Select>
      </div>

      {loading ? (
        <Loading />
      ) : (
        <>
          <DataTable<Payment>
            columns={columns}
            rows={rows}
            rowKey={(p) => p.id}
            actions={(p) =>
              p.status === "PENDING" ? (
                <>
                  <Button
                    size="sm"
                    variant="success"
                    disabled={busyId === p.id}
                    onClick={() => act(p, "approve")}
                  >
                    Duyệt
                  </Button>
                  <Button
                    size="sm"
                    variant="danger"
                    disabled={busyId === p.id}
                    onClick={() => act(p, "reject")}
                  >
                    Từ chối
                  </Button>
                </>
              ) : (
                <span className="text-xs text-gray-500">—</span>
              )
            }
          />

          <div className="mt-4 flex items-center justify-between text-sm text-gray-400">
            <span>Trang {page + 1}</span>
            <div className="flex gap-2">
              <Button
                size="sm"
                variant="secondary"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Trước
              </Button>
              <Button
                size="sm"
                variant="secondary"
                disabled={rows.length < PAGE_SIZE}
                onClick={() => setPage((p) => p + 1)}
              >
                Sau
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
