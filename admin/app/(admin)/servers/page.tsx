"use client";

import { useCallback, useEffect, useState } from "react";
import { apiGet, apiPost, apiPut } from "@/lib/api";
import DataTable, { Column } from "@/components/DataTable";
import Modal from "@/components/Modal";
import Button from "@/components/Button";
import Loading from "@/components/Loading";
import Alert from "@/components/Alert";
import PageHeader from "@/components/PageHeader";
import FormField, { Select, TextInput } from "@/components/FormField";

interface GameServer {
  id: number;
  code: string;
  name: string;
  host: string;
  tcpPort: number;
  status: string;
  crossGroup?: string;
  mergedInto?: number;
  sortOrder: number;
}

const emptyServer = {
  code: "",
  name: "",
  host: "",
  tcpPort: "",
  status: "ONLINE",
  sortOrder: "0",
};

export default function ServersPage() {
  const [rows, setRows] = useState<GameServer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<Record<string, any>>(emptyServer);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  // merge
  const [mergeOpen, setMergeOpen] = useState(false);
  const [mergeSource, setMergeSource] = useState("");
  const [mergeTarget, setMergeTarget] = useState("");
  const [mergeError, setMergeError] = useState("");

  // cross
  const [crossOpen, setCrossOpen] = useState(false);
  const [crossGroup, setCrossGroup] = useState("");
  const [crossIds, setCrossIds] = useState<number[]>([]);
  const [crossError, setCrossError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await apiGet<GameServer[]>("/api/admin/servers");
      setRows(Array.isArray(data) ? data : []);
    } catch (e: any) {
      setError(e.message || "Không tải được danh sách máy chủ");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  function openCreate() {
    setEditingId(null);
    setForm(emptyServer);
    setFormError("");
    setFormOpen(true);
  }

  function openEdit(s: GameServer) {
    setEditingId(s.id);
    setForm({
      code: s.code,
      name: s.name,
      host: s.host,
      tcpPort: String(s.tcpPort ?? ""),
      status: s.status,
      sortOrder: String(s.sortOrder ?? 0),
    });
    setFormError("");
    setFormOpen(true);
  }

  async function saveServer() {
    setFormError("");
    if (!form.code || !form.name || !form.host) {
      setFormError("Vui lòng nhập mã, tên và host");
      return;
    }
    const payload = {
      code: form.code,
      name: form.name,
      host: form.host,
      tcpPort: Number(form.tcpPort) || 0,
      status: form.status,
      sortOrder: Number(form.sortOrder) || 0,
    };
    setSaving(true);
    try {
      if (editingId != null) {
        await apiPut(`/api/admin/servers/${editingId}`, payload);
        setNotice("Đã cập nhật máy chủ");
      } else {
        await apiPost("/api/admin/servers", payload);
        setNotice("Đã tạo máy chủ");
      }
      setFormOpen(false);
      await load();
    } catch (e: any) {
      setFormError(e.message || "Lưu thất bại");
    } finally {
      setSaving(false);
    }
  }

  async function doMerge() {
    setMergeError("");
    if (!mergeSource || !mergeTarget) {
      setMergeError("Chọn cả máy chủ nguồn và đích");
      return;
    }
    if (mergeSource === mergeTarget) {
      setMergeError("Máy chủ nguồn và đích phải khác nhau");
      return;
    }
    try {
      await apiPost("/api/admin/servers/merge", {
        sourceId: Number(mergeSource),
        targetId: Number(mergeTarget),
      });
      setNotice("Đã gộp máy chủ");
      setMergeOpen(false);
      setMergeSource("");
      setMergeTarget("");
      await load();
    } catch (e: any) {
      setMergeError(e.message || "Gộp thất bại");
    }
  }

  async function doCross() {
    setCrossError("");
    if (!crossGroup) {
      setCrossError("Nhập tên nhóm liên server");
      return;
    }
    if (crossIds.length === 0) {
      setCrossError("Chọn ít nhất một máy chủ");
      return;
    }
    try {
      await apiPost("/api/admin/servers/cross", {
        crossGroup,
        serverIds: crossIds,
      });
      setNotice("Đã thiết lập liên server");
      setCrossOpen(false);
      setCrossGroup("");
      setCrossIds([]);
      await load();
    } catch (e: any) {
      setCrossError(e.message || "Thiết lập thất bại");
    }
  }

  function toggleCross(id: number) {
    setCrossIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  }

  const columns: Column<GameServer>[] = [
    { key: "id", header: "ID", width: "60px" },
    { key: "code", header: "Mã" },
    {
      key: "name",
      header: "Tên",
      render: (s) => <span className="font-medium text-white">{s.name}</span>,
    },
    { key: "host", header: "Host" },
    { key: "tcpPort", header: "Cổng" },
    {
      key: "status",
      header: "Trạng thái",
      render: (s) => (
        <span
          className={`rounded px-2 py-0.5 text-xs ${
            s.status === "ONLINE"
              ? "bg-emerald-500/20 text-emerald-300"
              : "bg-gray-500/20 text-gray-300"
          }`}
        >
          {s.status}
        </span>
      ),
    },
    { key: "crossGroup", header: "Liên server", render: (s) => s.crossGroup || "—" },
    {
      key: "mergedInto",
      header: "Gộp vào",
      render: (s) => (s.mergedInto ? `#${s.mergedInto}` : "—"),
    },
    { key: "sortOrder", header: "Thứ tự" },
  ];

  return (
    <div>
      <PageHeader
        title="Máy chủ"
        description="Quản lý máy chủ game, gộp server và liên server"
        actions={
          <>
            <Button variant="secondary" onClick={() => setMergeOpen(true)}>
              Gộp server
            </Button>
            <Button variant="secondary" onClick={() => setCrossOpen(true)}>
              Liên server
            </Button>
            <Button onClick={openCreate}>Thêm máy chủ</Button>
          </>
        }
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

      {loading ? (
        <Loading />
      ) : (
        <DataTable<GameServer>
          columns={columns}
          rows={rows}
          rowKey={(s) => s.id}
          actions={(s) => (
            <Button size="sm" variant="secondary" onClick={() => openEdit(s)}>
              Sửa
            </Button>
          )}
        />
      )}

      {/* Form modal */}
      <Modal
        open={formOpen}
        title={editingId != null ? "Sửa máy chủ" : "Thêm máy chủ"}
        onClose={() => setFormOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setFormOpen(false)}>
              Huỷ
            </Button>
            <Button onClick={saveServer} disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu"}
            </Button>
          </>
        }
      >
        {formError && <Alert kind="error">{formError}</Alert>}
        <div className="grid grid-cols-2 gap-x-4">
          <FormField label="Mã máy chủ" required>
            <TextInput
              value={form.code}
              onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))}
              placeholder="vd: s1"
            />
          </FormField>
          <FormField label="Tên hiển thị" required>
            <TextInput
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              placeholder="vd: Máy chủ 1 - Khởi Nguyên"
            />
          </FormField>
          <FormField label="Host" required>
            <TextInput
              value={form.host}
              onChange={(e) => setForm((f) => ({ ...f, host: e.target.value }))}
              placeholder="vd: 127.0.0.1"
            />
          </FormField>
          <FormField label="Cổng TCP">
            <TextInput
              type="number"
              value={form.tcpPort}
              onChange={(e) =>
                setForm((f) => ({ ...f, tcpPort: e.target.value }))
              }
            />
          </FormField>
          <FormField label="Trạng thái">
            <Select
              value={form.status}
              onChange={(e) =>
                setForm((f) => ({ ...f, status: e.target.value }))
              }
            >
              <option value="ONLINE">ONLINE</option>
              <option value="OFFLINE">OFFLINE</option>
              <option value="MAINTENANCE">MAINTENANCE</option>
            </Select>
          </FormField>
          <FormField label="Thứ tự sắp xếp">
            <TextInput
              type="number"
              value={form.sortOrder}
              onChange={(e) =>
                setForm((f) => ({ ...f, sortOrder: e.target.value }))
              }
            />
          </FormField>
        </div>
      </Modal>

      {/* Merge modal */}
      <Modal
        open={mergeOpen}
        title="Gộp server"
        onClose={() => setMergeOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setMergeOpen(false)}>
              Huỷ
            </Button>
            <Button onClick={doMerge}>Gộp</Button>
          </>
        }
      >
        {mergeError && <Alert kind="error">{mergeError}</Alert>}
        <p className="mb-3 text-xs text-gray-400">
          Máy chủ nguồn sẽ được gộp vào máy chủ đích.
        </p>
        <FormField label="Máy chủ nguồn" required>
          <Select
            value={mergeSource}
            onChange={(e) => setMergeSource(e.target.value)}
          >
            <option value="">— Chọn —</option>
            {rows.map((s) => (
              <option key={s.id} value={s.id}>
                #{s.id} {s.name}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Máy chủ đích" required>
          <Select
            value={mergeTarget}
            onChange={(e) => setMergeTarget(e.target.value)}
          >
            <option value="">— Chọn —</option>
            {rows.map((s) => (
              <option key={s.id} value={s.id}>
                #{s.id} {s.name}
              </option>
            ))}
          </Select>
        </FormField>
      </Modal>

      {/* Cross modal */}
      <Modal
        open={crossOpen}
        title="Liên server"
        onClose={() => setCrossOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setCrossOpen(false)}>
              Huỷ
            </Button>
            <Button onClick={doCross}>Lưu</Button>
          </>
        }
      >
        {crossError && <Alert kind="error">{crossError}</Alert>}
        <FormField label="Tên nhóm liên server" required>
          <TextInput
            value={crossGroup}
            onChange={(e) => setCrossGroup(e.target.value)}
            placeholder="vd: cross-1"
          />
        </FormField>
        <FormField label="Chọn các máy chủ trong nhóm">
          <div className="max-h-56 space-y-1 overflow-y-auto rounded-md border border-surface-border p-2">
            {rows.map((s) => (
              <label
                key={s.id}
                className="flex cursor-pointer items-center gap-2 rounded px-2 py-1 text-sm text-gray-300 hover:bg-surface-hover"
              >
                <input
                  type="checkbox"
                  checked={crossIds.includes(s.id)}
                  onChange={() => toggleCross(s.id)}
                  className="h-4 w-4 rounded border-surface-border bg-surface accent-brand"
                />
                #{s.id} {s.name} ({s.code})
              </label>
            ))}
          </div>
        </FormField>
      </Modal>
    </div>
  );
}
