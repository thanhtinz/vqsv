"use client";

import { useCallback, useEffect, useState } from "react";
import { apiGet, apiPatch, apiPost } from "@/lib/api";
import DataTable, { Column } from "@/components/DataTable";
import Modal from "@/components/Modal";
import Button from "@/components/Button";
import Loading from "@/components/Loading";
import Alert from "@/components/Alert";
import PageHeader from "@/components/PageHeader";
import FormField, { Select, TextInput } from "@/components/FormField";
import { formatNumber, formatVnd } from "@/lib/format";

interface AccountSummary {
  id: number;
  username: string;
  email: string;
  role: string;
  balanceXu: number;
  totalTopup: number;
  status: string;
}

interface GameCharacter {
  id: number;
  name?: string;
  serverId?: number;
  level?: number;
  [k: string]: any;
}

const PAGE_SIZE = 20;

export default function UsersPage() {
  const [rows, setRows] = useState<AccountSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);

  // edit modal
  const [editing, setEditing] = useState<AccountSummary | null>(null);
  const [editForm, setEditForm] = useState({
    role: "",
    status: "",
    isBanned: false,
    banReason: "",
    balanceXuDelta: "",
  });
  const [saving, setSaving] = useState(false);
  const [editError, setEditError] = useState("");

  // password modal
  const [pwUser, setPwUser] = useState<AccountSummary | null>(null);
  const [newPassword, setNewPassword] = useState("");
  const [pwError, setPwError] = useState("");

  // characters modal
  const [charUser, setCharUser] = useState<AccountSummary | null>(null);
  const [chars, setChars] = useState<GameCharacter[] | null>(null);
  const [charError, setCharError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await apiGet<AccountSummary[]>(
        `/api/admin/users?q=${encodeURIComponent(q)}&page=${page}&size=${PAGE_SIZE}`
      );
      setRows(Array.isArray(data) ? data : []);
    } catch (e: any) {
      setError(e.message || "Không tải được danh sách");
    } finally {
      setLoading(false);
    }
  }, [q, page]);

  useEffect(() => {
    load();
  }, [load]);

  function onSearch(e: React.FormEvent) {
    e.preventDefault();
    setPage(0);
    load();
  }

  function openEdit(u: AccountSummary) {
    setEditing(u);
    setEditForm({
      role: u.role,
      status: u.status,
      isBanned: u.status === "BANNED",
      banReason: "",
      balanceXuDelta: "",
    });
    setEditError("");
  }

  async function saveEdit() {
    if (!editing) return;
    setEditError("");
    const payload: any = {};
    if (editForm.role) payload.role = editForm.role;
    if (editForm.status) payload.status = editForm.status;
    payload.isBanned = editForm.isBanned;
    if (editForm.banReason) payload.banReason = editForm.banReason;
    if (editForm.balanceXuDelta !== "") {
      const n = Number(editForm.balanceXuDelta);
      if (isNaN(n)) {
        setEditError("Số xu cộng/trừ không hợp lệ");
        return;
      }
      payload.balanceXuDelta = n;
    }
    setSaving(true);
    try {
      await apiPatch(`/api/admin/users/${editing.id}`, payload);
      setNotice(`Đã cập nhật tài khoản ${editing.username}`);
      setEditing(null);
      await load();
    } catch (e: any) {
      setEditError(e.message || "Cập nhật thất bại");
    } finally {
      setSaving(false);
    }
  }

  async function savePassword() {
    if (!pwUser) return;
    setPwError("");
    if (newPassword.length < 4) {
      setPwError("Mật khẩu phải có ít nhất 4 ký tự");
      return;
    }
    try {
      await apiPost(`/api/admin/users/${pwUser.id}/password`, {
        newPassword,
      });
      setNotice(`Đã đặt lại mật khẩu cho ${pwUser.username}`);
      setPwUser(null);
      setNewPassword("");
    } catch (e: any) {
      setPwError(e.message || "Đặt lại mật khẩu thất bại");
    }
  }

  async function openChars(u: AccountSummary) {
    setCharUser(u);
    setChars(null);
    setCharError("");
    try {
      const data = await apiGet<GameCharacter[]>(
        `/api/admin/users/${u.id}/characters`
      );
      setChars(Array.isArray(data) ? data : []);
    } catch (e: any) {
      setCharError(e.message || "Không tải được nhân vật");
      setChars([]);
    }
  }

  const columns: Column<AccountSummary>[] = [
    { key: "id", header: "ID", width: "60px" },
    {
      key: "username",
      header: "Tài khoản",
      render: (u) => <span className="font-medium text-white">{u.username}</span>,
    },
    { key: "email", header: "Email" },
    {
      key: "role",
      header: "Vai trò",
      render: (u) => (
        <span
          className={`rounded px-2 py-0.5 text-xs ${
            u.role === "ADMIN"
              ? "bg-violet-500/20 text-violet-300"
              : u.role === "GM"
              ? "bg-sky-500/20 text-sky-300"
              : "bg-surface-hover text-gray-300"
          }`}
        >
          {u.role}
        </span>
      ),
    },
    { key: "balanceXu", header: "Xu", render: (u) => formatNumber(u.balanceXu) },
    {
      key: "totalTopup",
      header: "Tổng nạp",
      render: (u) => formatVnd(u.totalTopup),
    },
    {
      key: "status",
      header: "Trạng thái",
      render: (u) => (
        <span
          className={`rounded px-2 py-0.5 text-xs ${
            u.status === "ACTIVE"
              ? "bg-emerald-500/20 text-emerald-300"
              : "bg-red-500/20 text-red-300"
          }`}
        >
          {u.status}
        </span>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Người dùng"
        description="Quản lý tài khoản, vai trò, số dư xu và khoá tài khoản"
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

      <form onSubmit={onSearch} className="mb-4 flex gap-2">
        <TextInput
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Tìm theo tài khoản / email..."
          className="max-w-xs"
        />
        <Button type="submit" variant="secondary">
          Tìm kiếm
        </Button>
      </form>

      {loading ? (
        <Loading />
      ) : (
        <>
          <DataTable<AccountSummary>
            columns={columns}
            rows={rows}
            rowKey={(u) => u.id}
            actions={(u) => (
              <>
                <Button size="sm" variant="secondary" onClick={() => openEdit(u)}>
                  Sửa
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => {
                    setPwUser(u);
                    setNewPassword("");
                    setPwError("");
                  }}
                >
                  Mật khẩu
                </Button>
                <Button size="sm" variant="ghost" onClick={() => openChars(u)}>
                  Nhân vật
                </Button>
              </>
            )}
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

      {/* Edit modal */}
      <Modal
        open={!!editing}
        title={editing ? `Sửa tài khoản: ${editing.username}` : ""}
        onClose={() => setEditing(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setEditing(null)}>
              Huỷ
            </Button>
            <Button onClick={saveEdit} disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu"}
            </Button>
          </>
        }
      >
        {editError && <Alert kind="error">{editError}</Alert>}
        <FormField label="Vai trò">
          <Select
            value={editForm.role}
            onChange={(e) =>
              setEditForm((f) => ({ ...f, role: e.target.value }))
            }
          >
            <option value="USER">USER</option>
            <option value="GM">GM</option>
            <option value="ADMIN">ADMIN</option>
          </Select>
        </FormField>
        <FormField label="Trạng thái">
          <Select
            value={editForm.status}
            onChange={(e) =>
              setEditForm((f) => ({ ...f, status: e.target.value }))
            }
          >
            <option value="ACTIVE">ACTIVE</option>
            <option value="BANNED">BANNED</option>
            <option value="LOCKED">LOCKED</option>
          </Select>
        </FormField>
        <div className="mb-3 flex items-center gap-2">
          <input
            id="isBanned"
            type="checkbox"
            checked={editForm.isBanned}
            onChange={(e) =>
              setEditForm((f) => ({ ...f, isBanned: e.target.checked }))
            }
            className="h-4 w-4 rounded border-surface-border bg-surface accent-brand"
          />
          <label htmlFor="isBanned" className="text-sm text-gray-300">
            Khoá tài khoản (ban)
          </label>
        </div>
        <FormField label="Lý do khoá">
          <TextInput
            value={editForm.banReason}
            onChange={(e) =>
              setEditForm((f) => ({ ...f, banReason: e.target.value }))
            }
            placeholder="Tuỳ chọn"
          />
        </FormField>
        <FormField
          label="Cộng/trừ xu"
          hint="Nhập số dương để cộng, số âm để trừ. Để trống nếu không đổi."
        >
          <TextInput
            type="number"
            value={editForm.balanceXuDelta}
            onChange={(e) =>
              setEditForm((f) => ({ ...f, balanceXuDelta: e.target.value }))
            }
            placeholder="vd: 1000 hoặc -500"
          />
        </FormField>
      </Modal>

      {/* Password modal */}
      <Modal
        open={!!pwUser}
        title={pwUser ? `Đặt lại mật khẩu: ${pwUser.username}` : ""}
        onClose={() => setPwUser(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setPwUser(null)}>
              Huỷ
            </Button>
            <Button onClick={savePassword}>Đặt lại</Button>
          </>
        }
      >
        {pwError && <Alert kind="error">{pwError}</Alert>}
        <FormField label="Mật khẩu mới" required>
          <TextInput
            type="text"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="Mật khẩu mới"
          />
        </FormField>
      </Modal>

      {/* Characters modal */}
      <Modal
        open={!!charUser}
        wide
        title={charUser ? `Nhân vật của ${charUser.username}` : ""}
        onClose={() => setCharUser(null)}
      >
        {charError && <Alert kind="error">{charError}</Alert>}
        {chars === null ? (
          <Loading />
        ) : chars.length === 0 ? (
          <p className="py-6 text-center text-gray-500">
            Tài khoản chưa có nhân vật
          </p>
        ) : (
          <DataTable<GameCharacter>
            columns={[
              { key: "id", header: "ID", width: "60px" },
              { key: "name", header: "Tên" },
              { key: "serverId", header: "Máy chủ" },
              { key: "level", header: "Cấp" },
            ]}
            rows={chars}
            rowKey={(c) => c.id}
          />
        )}
      </Modal>
    </div>
  );
}
