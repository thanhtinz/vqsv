"use client";

import { useCallback, useEffect, useState } from "react";
import { apiGet } from "@/lib/api";
import DataTable, { Column } from "@/components/DataTable";
import Button from "@/components/Button";
import Loading from "@/components/Loading";
import Alert from "@/components/Alert";
import PageHeader from "@/components/PageHeader";
import { formatDate } from "@/lib/format";

interface AuditLog {
  id: number;
  actorId: number;
  actorName: string;
  action: string;
  target: string;
  detail: string;
  ip: string;
  createdAt: string;
}

const PAGE_SIZE = 50;

export default function AuditPage() {
  const [rows, setRows] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [page, setPage] = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await apiGet<AuditLog[]>(
        `/api/admin/audit?page=${page}&size=${PAGE_SIZE}`
      );
      setRows(Array.isArray(data) ? data : []);
    } catch (e: any) {
      setError(e.message || "Không tải được nhật ký");
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const columns: Column<AuditLog>[] = [
    { key: "id", header: "ID", width: "60px" },
    {
      key: "actorName",
      header: "Người thực hiện",
      render: (a) => (
        <span className="text-white">
          {a.actorName} <span className="text-gray-500">#{a.actorId}</span>
        </span>
      ),
    },
    {
      key: "action",
      header: "Hành động",
      render: (a) => (
        <span className="rounded bg-surface-hover px-2 py-0.5 text-xs text-brand-light">
          {a.action}
        </span>
      ),
    },
    { key: "target", header: "Đối tượng" },
    {
      key: "detail",
      header: "Chi tiết",
      render: (a) => (
        <span className="block max-w-md truncate text-gray-400" title={a.detail}>
          {a.detail}
        </span>
      ),
    },
    { key: "ip", header: "IP" },
    {
      key: "createdAt",
      header: "Thời gian",
      render: (a) => formatDate(a.createdAt),
    },
  ];

  return (
    <div>
      <PageHeader title="Nhật ký" description="Lịch sử thao tác của quản trị viên" />

      {error && (
        <Alert kind="error" onClose={() => setError("")}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Loading />
      ) : (
        <>
          <DataTable<AuditLog>
            columns={columns}
            rows={rows}
            rowKey={(a) => a.id}
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
