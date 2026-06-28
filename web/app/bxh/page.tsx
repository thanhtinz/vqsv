"use client";

import { useEffect, useMemo, useState } from "react";
import Card, { SectionTitle } from "@/components/Card";
import { Select } from "@/components/Input";
import { Loading, ErrorMessage, EmptyState } from "@/components/Loading";
import { apiGet, ApiError } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type { LeaderboardRow, ServerItem } from "@/lib/types";

function rankBadge(rank: number) {
  if (rank === 1) return "bg-yellow-400/20 text-yellow-300";
  if (rank === 2) return "bg-slate-300/20 text-slate-200";
  if (rank === 3) return "bg-amber-700/30 text-amber-400";
  return "bg-white/5 text-white/60";
}

export default function LeaderboardPage() {
  const [servers, setServers] = useState<ServerItem[]>([]);
  const [serverId, setServerId] = useState<string>("");
  const [crossServer, setCrossServer] = useState(false);

  const [rows, setRows] = useState<LeaderboardRow[] | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiGet<ServerItem[]>("/api/web/public/servers")
      .then(setServers)
      .catch(() => {
        /* selector optional */
      });
  }, []);

  const crossGroup = useMemo(() => {
    if (!crossServer || !serverId) return "";
    const s = servers.find((x) => String(x.id) === serverId);
    return s?.crossGroup || "";
  }, [crossServer, serverId, servers]);

  useEffect(() => {
    setLoading(true);
    setError("");
    const params = new URLSearchParams();
    params.set("limit", "50");
    if (crossServer) {
      if (crossGroup) params.set("crossGroup", crossGroup);
    } else if (serverId) {
      params.set("serverId", serverId);
    }
    apiGet<LeaderboardRow[]>(
      `/api/web/public/leaderboard?${params.toString()}`
    )
      .then(setRows)
      .catch((e) =>
        setError(
          e instanceof ApiError ? e.message : "Không tải được bảng xếp hạng."
        )
      )
      .finally(() => setLoading(false));
  }, [serverId, crossServer, crossGroup]);

  return (
    <div className="mx-auto max-w-4xl px-4 py-12">
      <SectionTitle
        title="Bảng Xếp Hạng"
        subtitle="Top cao thủ mạnh nhất của VQSV."
      />

      <Card className="mb-6 flex flex-wrap items-end gap-4">
        <div className="w-full sm:w-64">
          <Select
            id="server"
            label="Máy chủ"
            value={serverId}
            onChange={(e) => setServerId(e.target.value)}
            disabled={crossServer}
          >
            <option value="">Tất cả máy chủ</option>
            {servers.map((s) => (
              <option key={s.id} value={s.id}>
                {s.code} — {s.name}
              </option>
            ))}
          </Select>
        </div>
        <label className="flex cursor-pointer items-center gap-2 pb-2.5 text-sm text-white/80">
          <input
            type="checkbox"
            checked={crossServer}
            onChange={(e) => setCrossServer(e.target.checked)}
            className="h-4 w-4 accent-brand"
          />
          Xếp hạng liên server
        </label>
      </Card>

      {error ? (
        <ErrorMessage message={error} />
      ) : loading ? (
        <Loading label="Đang tải bảng xếp hạng..." />
      ) : !rows || rows.length === 0 ? (
        <EmptyState message="Chưa có dữ liệu xếp hạng." />
      ) : (
        <Card className="overflow-x-auto p-0">
          <table className="w-full min-w-[480px] text-left text-sm">
            <thead className="border-b border-white/10 text-white/50">
              <tr>
                <th className="px-4 py-3 font-medium">Hạng</th>
                <th className="px-4 py-3 font-medium">Nhân vật</th>
                <th className="px-4 py-3 font-medium">Cấp</th>
                <th className="px-4 py-3 font-medium">Kinh nghiệm</th>
                <th className="px-4 py-3 font-medium">Máy chủ</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr
                  key={`${r.serverCode}-${r.rank}-${r.name}`}
                  className="border-b border-white/5 last:border-0"
                >
                  <td className="px-4 py-3">
                    <span
                      className={`inline-grid h-7 w-7 place-items-center rounded-full text-xs font-bold ${rankBadge(
                        r.rank
                      )}`}
                    >
                      {r.rank}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-medium text-white">
                    {r.name}
                  </td>
                  <td className="px-4 py-3 text-brand">Lv.{r.level}</td>
                  <td className="px-4 py-3 text-white/60">
                    {formatNumber(r.exp)}
                  </td>
                  <td className="px-4 py-3 font-mono text-white/60">
                    {r.serverCode}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  );
}
