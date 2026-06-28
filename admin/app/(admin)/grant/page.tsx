"use client";

import { useState } from "react";
import { apiPost } from "@/lib/api";
import Button from "@/components/Button";
import Alert from "@/components/Alert";
import PageHeader from "@/components/PageHeader";
import FormField, { TextArea, TextInput } from "@/components/FormField";

const SAMPLE = `{
  "xu": 1000,
  "gold": 50000,
  "items": [
    { "itemId": 1, "qty": 10 }
  ]
}`;

export default function GrantPage() {
  const [serverId, setServerId] = useState("");
  const [playerId, setPlayerId] = useState("");
  const [rewardJson, setRewardJson] = useState(SAMPLE);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setNotice("");
    if (!serverId || !playerId) {
      setError("Vui lòng nhập máy chủ và ID nhân vật");
      return;
    }
    let parsed: any;
    try {
      parsed = JSON.parse(rewardJson);
    } catch {
      setError("rewardJson không phải JSON hợp lệ");
      return;
    }
    setSaving(true);
    try {
      await apiPost("/api/admin/grant", {
        serverId: Number(serverId),
        playerId: Number(playerId),
        rewardJson: JSON.stringify(parsed),
      });
      setNotice("Đã gửi phần thưởng thành công");
    } catch (e: any) {
      setError(e.message || "Tặng quà thất bại");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <PageHeader
        title="Tặng quà"
        description="Gửi phần thưởng trực tiếp tới một nhân vật trong game"
      />

      <form
        onSubmit={submit}
        className="max-w-2xl rounded-xl border border-surface-border bg-surface-card p-6"
      >
        {error && <Alert kind="error">{error}</Alert>}
        {notice && <Alert kind="success">{notice}</Alert>}

        <div className="grid grid-cols-2 gap-x-4">
          <FormField label="Máy chủ (serverId)" required>
            <TextInput
              type="number"
              value={serverId}
              onChange={(e) => setServerId(e.target.value)}
              placeholder="vd: 1"
            />
          </FormField>
          <FormField label="ID nhân vật (playerId)" required>
            <TextInput
              type="number"
              value={playerId}
              onChange={(e) => setPlayerId(e.target.value)}
              placeholder="vd: 1024"
            />
          </FormField>
        </div>

        <FormField
          label="Phần thưởng (rewardJson)"
          required
          hint={
            <>
              Định dạng JSON, vd:{" "}
              <code>
                {`{"xu":..,"gold":..,"items":[{"itemId":..,"qty":..}]}`}
              </code>
            </>
          }
        >
          <TextArea
            rows={8}
            value={rewardJson}
            onChange={(e) => setRewardJson(e.target.value)}
          />
        </FormField>

        <Button type="submit" disabled={saving} className="mt-2">
          {saving ? "Đang gửi..." : "Gửi phần thưởng"}
        </Button>
      </form>
    </div>
  );
}
