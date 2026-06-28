"use client";

import { ReactNode, useEffect, useState } from "react";
import { apiGet, apiPost, apiDelete } from "@/lib/api";
import DataTable, { Column } from "./DataTable";
import Modal from "./Modal";
import Button from "./Button";
import Alert from "./Alert";
import PageHeader from "./PageHeader";
import FormField, { TextInput, TextArea, Select } from "./FormField";
import RewardInput from "./RewardInput";

export type FieldType =
  | "text"
  | "number"
  | "textarea"
  | "select"
  | "checkbox"
  | "datetime"
  | "reward";

export interface Field {
  name: string;
  label: string;
  type?: FieldType;
  required?: boolean;
  defaultValue?: any;
  placeholder?: string;
  hint?: ReactNode;
  options?: { value: string | number; label: string }[];
}

interface CrudResourceProps<T> {
  title: string;
  description?: string;
  /** REST base, e.g. /api/admin/game/pets. GET=list, POST=create/update (upsert), DELETE /{id}. */
  basePath: string;
  rowKey: (row: T) => string | number;
  columns: Column<T>[];
  fields: Field[];
  createLabel?: string;
  /** show an edit button per row (server upserts via POST). Default true. */
  editable?: boolean;
  /**
   * Accepted for backward-compatibility. Create and update always go through
   * POST basePath (the server's save() upserts by id), so this is a no-op.
   */
  upsertViaPost?: boolean;
  /** widen the create/edit modal. */
  wide?: boolean;
  /** transform the form values before sending to the server. */
  transformOut?: (form: Record<string, any>) => any;
  /** transform a row into form values when opening the edit modal. */
  transformIn?: (row: T) => Record<string, any>;
}

// --- datetime helpers: <input type="datetime-local"> <-> ISO/Instant string ---
function toLocalInput(iso: any): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (isNaN(d.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}`;
}
function toIso(local: string): string | null {
  if (!local) return null;
  const d = new Date(local);
  return isNaN(d.getTime()) ? null : d.toISOString();
}

export default function CrudResource<T extends Record<string, any>>({
  title,
  description,
  basePath,
  rowKey,
  columns,
  fields,
  createLabel = "Tạo mới",
  editable = true,
  upsertViaPost: _upsertViaPost,
  wide,
  transformOut,
  transformIn,
}: CrudResourceProps<T>) {
  const [rows, setRows] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const [open, setOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | number | null>(null);
  const [form, setForm] = useState<Record<string, any>>({});
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    setError("");
    try {
      const data = await apiGet<T[]>(basePath);
      setRows(Array.isArray(data) ? data : []);
    } catch (e: any) {
      setError(e.message || "Không tải được dữ liệu");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [basePath]);

  function blankForm(): Record<string, any> {
    const f: Record<string, any> = {};
    for (const fl of fields) {
      f[fl.name] =
        fl.defaultValue !== undefined
          ? fl.defaultValue
          : fl.type === "checkbox"
          ? false
          : "";
    }
    return f;
  }

  function openCreate() {
    setEditingId(null);
    setForm(blankForm());
    setOpen(true);
  }

  function openEdit(row: T) {
    setEditingId(rowKey(row));
    const base = transformIn ? transformIn(row) : { ...row };
    // normalise datetime fields back to the input format
    for (const fl of fields) {
      if (fl.type === "datetime") base[fl.name] = toLocalInput(base[fl.name]);
      if (base[fl.name] === undefined || base[fl.name] === null) {
        base[fl.name] =
          fl.defaultValue !== undefined
            ? fl.defaultValue
            : fl.type === "checkbox"
            ? false
            : "";
      }
    }
    setForm(base);
    setOpen(true);
  }

  function setField(name: string, value: any) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function buildPayload(): any {
    const out: Record<string, any> = { ...form };
    for (const fl of fields) {
      const v = form[fl.name];
      switch (fl.type) {
        case "number":
          out[fl.name] =
            v === "" || v === null || v === undefined ? fl.defaultValue ?? null : Number(v);
          break;
        case "checkbox":
          out[fl.name] = !!v;
          break;
        case "datetime": {
          // Omit empty datetimes entirely so the server applies its own default
          // (e.g. a non-nullable startsAt) instead of receiving null and 400-ing.
          const iso = toIso(v);
          if (iso === null) delete out[fl.name];
          else out[fl.name] = iso;
          break;
        }
        default:
          out[fl.name] = v;
      }
    }
    if (editingId !== null && out.id === undefined) out.id = editingId;
    return transformOut ? transformOut(out) : out;
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setNotice("");
    // required-field check
    for (const fl of fields) {
      if (fl.required && fl.type !== "checkbox") {
        const v = form[fl.name];
        if (v === "" || v === null || v === undefined) {
          setError(`Vui lòng nhập: ${fl.label}`);
          return;
        }
      }
    }
    let payload: any;
    try {
      payload = buildPayload();
    } catch (err: any) {
      setError(err.message || "Dữ liệu không hợp lệ");
      return;
    }
    setSaving(true);
    try {
      await apiPost(basePath, payload);
      setOpen(false);
      setNotice(editingId !== null ? "Đã cập nhật" : "Đã tạo mới");
      await load();
    } catch (err: any) {
      setError(err.message || "Lưu thất bại");
    } finally {
      setSaving(false);
    }
  }

  async function remove(row: T) {
    if (!confirm("Xoá mục này?")) return;
    setError("");
    try {
      await apiDelete(`${basePath}/${rowKey(row)}`);
      setNotice("Đã xoá");
      await load();
    } catch (e: any) {
      setError(e.message || "Xoá thất bại");
    }
  }

  function renderInput(fl: Field) {
    const v = form[fl.name];
    const common = {
      id: fl.name,
      placeholder: fl.placeholder,
      required: fl.required,
    };
    switch (fl.type) {
      case "number":
        return (
          <TextInput
            {...common}
            type="number"
            value={v ?? ""}
            onChange={(e) => setField(fl.name, e.target.value)}
          />
        );
      case "textarea":
        return (
          <TextArea
            {...common}
            rows={5}
            value={v ?? ""}
            onChange={(e) => setField(fl.name, e.target.value)}
          />
        );
      case "select":
        return (
          <Select
            {...common}
            value={v ?? ""}
            onChange={(e) => setField(fl.name, e.target.value)}
          >
            {(fl.options || []).map((o) => (
              <option key={String(o.value)} value={o.value}>
                {o.label}
              </option>
            ))}
          </Select>
        );
      case "checkbox":
        return (
          <label className="flex items-center gap-2 text-sm text-gray-200">
            <input
              id={fl.name}
              type="checkbox"
              className="h-4 w-4 rounded border-surface-border bg-surface"
              checked={!!v}
              onChange={(e) => setField(fl.name, e.target.checked)}
            />
            <span>{fl.placeholder || "Bật"}</span>
          </label>
        );
      case "datetime":
        return (
          <TextInput
            {...common}
            type="datetime-local"
            value={v ?? ""}
            onChange={(e) => setField(fl.name, e.target.value)}
          />
        );
      case "reward":
        return (
          <RewardInput value={v ?? ""} onChange={(json) => setField(fl.name, json)} />
        );
      default:
        return (
          <TextInput
            {...common}
            type="text"
            value={v ?? ""}
            onChange={(e) => setField(fl.name, e.target.value)}
          />
        );
    }
  }

  return (
    <div>
      <PageHeader
        title={title}
        description={description}
        actions={<Button onClick={openCreate}>{createLabel}</Button>}
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
        <p className="text-sm text-gray-400">Đang tải...</p>
      ) : (
        <DataTable<T>
          columns={columns}
          rows={rows}
          rowKey={rowKey}
          actions={(row) => (
            <>
              {editable && (
                <Button variant="secondary" size="sm" onClick={() => openEdit(row)}>
                  Sửa
                </Button>
              )}
              <Button variant="danger" size="sm" onClick={() => remove(row)}>
                Xoá
              </Button>
            </>
          )}
        />
      )}

      <Modal
        open={open}
        wide={wide}
        title={editingId !== null ? `Sửa ${title}` : createLabel}
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setOpen(false)}>
              Huỷ
            </Button>
            <Button form="crud-form" type="submit" disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu"}
            </Button>
          </>
        }
      >
        <form id="crud-form" onSubmit={submit}>
          {fields.map((fl) => (
            <FormField
              key={fl.name}
              label={fl.label}
              htmlFor={fl.name}
              required={fl.required}
              hint={fl.hint}
            >
              {renderInput(fl)}
            </FormField>
          ))}
        </form>
      </Modal>
    </div>
  );
}
