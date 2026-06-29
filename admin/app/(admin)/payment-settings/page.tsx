"use client";

import { useEffect, useState } from "react";
import { apiGet, apiPut } from "@/lib/api";
import Button from "@/components/Button";
import Alert from "@/components/Alert";
import PageHeader from "@/components/PageHeader";
import FormField, { TextInput } from "@/components/FormField";

interface PaymentSettings {
  enabled: boolean;
  sepayApiKey: string;
  bankAccount: string;
  bankCode: string;
  accountHolder: string;
  prefix: string;
}

const EMPTY: PaymentSettings = {
  enabled: false,
  sepayApiKey: "",
  bankAccount: "",
  bankCode: "",
  accountHolder: "",
  prefix: "VQSV",
};

export default function PaymentSettingsPage() {
  const [form, setForm] = useState<PaymentSettings>(EMPTY);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    apiGet<PaymentSettings>("/api/admin/payment-settings")
      .then((s) => setForm({ ...EMPTY, ...s }))
      .catch((e) => setError(e.message || "Không tải được cấu hình"))
      .finally(() => setLoading(false));
  }, []);

  function set<K extends keyof PaymentSettings>(k: K, v: PaymentSettings[K]) {
    setForm((p) => ({ ...p, [k]: v }));
  }

  async function save() {
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const saved = await apiPut<PaymentSettings>("/api/admin/payment-settings", form);
      setForm({ ...EMPTY, ...saved });
      setNotice("Đã lưu cấu hình thanh toán");
    } catch (e: any) {
      setError(e.message || "Lưu thất bại");
    } finally {
      setSaving(false);
    }
  }

  const webhookUrl =
    (typeof window !== "undefined" ? window.location.origin.replace(/\/$/, "") : "") +
    "/api/web/public/topup/sepay/webhook";

  return (
    <div>
      <PageHeader
        title="Cấu hình thanh toán (SePay)"
        description="Nạp tiền qua chuyển khoản ngân hàng + webhook SePay. Cấu hình tại đây, không cần sửa code."
      />

      {error && <Alert kind="error" onClose={() => setError("")}>{error}</Alert>}
      {notice && <Alert kind="success" onClose={() => setNotice("")}>{notice}</Alert>}

      {loading ? (
        <p className="text-sm text-gray-400">Đang tải...</p>
      ) : (
        <div className="max-w-2xl rounded-xl border border-surface-border bg-surface-card p-6">
          <FormField label="Bật cổng SePay">
            <label className="flex items-center gap-2 text-sm text-gray-200">
              <input
                type="checkbox"
                className="h-4 w-4"
                checked={form.enabled}
                onChange={(e) => set("enabled", e.target.checked)}
              />
              <span>Khi tắt, web dùng luồng nạp thủ công (admin duyệt).</span>
            </label>
          </FormField>

          <FormField label="SePay API Key" hint="Khoá SePay gửi kèm trong webhook (Authorization: Apikey ...)">
            <TextInput value={form.sepayApiKey} onChange={(e) => set("sepayApiKey", e.target.value)} placeholder="vd: a1b2c3..." />
          </FormField>

          <FormField label="Số tài khoản nhận tiền" required>
            <TextInput value={form.bankAccount} onChange={(e) => set("bankAccount", e.target.value)} placeholder="vd: 0123456789" />
          </FormField>

          <FormField label="Mã ngân hàng (VietQR)" hint="vd: MBBank, VCB, ACB, TCB...">
            <TextInput value={form.bankCode} onChange={(e) => set("bankCode", e.target.value)} placeholder="vd: MBBank" />
          </FormField>

          <FormField label="Tên chủ tài khoản">
            <TextInput value={form.accountHolder} onChange={(e) => set("accountHolder", e.target.value)} placeholder="vd: NGUYEN VAN A" />
          </FormField>

          <FormField label="Tiền tố nội dung chuyển khoản" hint="Nội dung CK sẽ là tiền tố + mã giao dịch, vd VQSV123">
            <TextInput value={form.prefix} onChange={(e) => set("prefix", e.target.value)} placeholder="VQSV" />
          </FormField>

          <div className="mt-2 rounded-md border border-surface-border bg-surface p-3 text-xs text-gray-400">
            Khai báo URL webhook này trong bảng điều khiển SePay:
            <code className="ml-1 break-all text-brand-light">{webhookUrl}</code>
          </div>

          <div className="mt-4">
            <Button onClick={save} disabled={saving}>
              {saving ? "Đang lưu..." : "Lưu cấu hình"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
