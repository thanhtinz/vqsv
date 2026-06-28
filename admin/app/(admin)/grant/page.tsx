"use client";

import { useState } from "react";
import { apiPost } from "@/lib/api";
import Button from "@/components/Button";
import Alert from "@/components/Alert";
import PageHeader from "@/components/PageHeader";
import FormField, { TextInput } from "@/components/FormField";
import RewardInput from "@/components/RewardInput";
import { REWARD_HINT } from "@/lib/reward";

export default function GrantPage() {
  const [serverId, setServerId] = useState("");
  const [playerId, setPlayerId] = useState("");
  const [rewardJson, setRewardJson] = useState('{"xu":0,"gold":0,"items":[]}');
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
    setSaving(true);
    try {
      await apiPost("/api/admin/grant", {
        serverId: Number(serverId),
        playerId: Number(playerId),
        rewardJson,
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

        <FormField label="Phần thưởng" required hint={REWARD_HINT}>
          <RewardInput value={rewardJson} onChange={setRewardJson} />
        </FormField>

        <Button type="submit" disabled={saving} className="mt-2">
          {saving ? "Đang gửi..." : "Gửi phần thưởng"}
        </Button>
      </form>
    </div>
  );
}
