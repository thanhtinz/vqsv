"use client";

import { useEffect, useState, type FormEvent } from "react";
import AuthGuard from "@/components/AuthGuard";
import Card from "@/components/Card";
import Button from "@/components/Button";
import { Input, Select } from "@/components/Input";
import { ErrorMessage } from "@/components/Loading";
import { apiGet, apiPost, ApiError } from "@/lib/api";
import type { CharacterItem, RedeemResult } from "@/lib/types";

function GiftcodeInner() {
  const [code, setCode] = useState("");
  const [chars, setChars] = useState<CharacterItem[]>([]);
  const [playerId, setPlayerId] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState<RedeemResult | null>(null);

  useEffect(() => {
    apiGet<CharacterItem[]>("/api/web/profile/characters", { auth: true })
      .then((c) => {
        setChars(c);
        if (c.length > 0) setPlayerId(String(c[0].id));
      })
      .catch(() => {
        /* characters optional for some codes */
      });
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setResult(null);
    if (!code.trim()) {
      setError("Vui lòng nhập giftcode.");
      return;
    }
    setLoading(true);
    try {
      const res = await apiPost<RedeemResult>(
        "/api/web/giftcode/redeem",
        {
          code: code.trim(),
          playerId: playerId ? Number(playerId) : undefined,
        },
        { auth: true }
      );
      setResult(res);
      if (res.success) setCode("");
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : "Đổi giftcode thất bại."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-16">
      <h1 className="text-3xl font-black text-white">Nhập Giftcode</h1>
      <p className="mt-2 text-sm text-white/60">
        Nhập mã quà tặng và chọn nhân vật để nhận thưởng trong game.
      </p>

      <Card className="mt-8">
        <form onSubmit={onSubmit} className="flex flex-col gap-4">
          {error && <ErrorMessage message={error} />}
          {result && (
            <div
              className={`rounded-lg border px-4 py-3 text-sm ${
                result.success
                  ? "border-emerald-500/40 bg-emerald-500/10 text-emerald-300"
                  : "border-red-500/40 bg-red-500/10 text-red-300"
              }`}
            >
              <p className="font-semibold">{result.message}</p>
              {result.reward && (
                <p className="mt-1 text-white/70">
                  Phần thưởng: {result.reward}
                </p>
              )}
            </div>
          )}

          <Input
            id="code"
            label="Giftcode"
            placeholder="VD: VQSV2026"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            required
          />

          {chars.length > 0 ? (
            <Select
              id="player"
              label="Nhân vật nhận thưởng"
              value={playerId}
              onChange={(e) => setPlayerId(e.target.value)}
            >
              {chars.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} (Lv.{c.level}) — {c.serverName}
                </option>
              ))}
            </Select>
          ) : (
            <p className="text-xs text-white/40">
              Không tìm thấy nhân vật. Một số mã vẫn có thể áp dụng cho tài
              khoản.
            </p>
          )}

          <Button type="submit" disabled={loading} className="mt-1">
            {loading ? "Đang xử lý..." : "Đổi quà"}
          </Button>
        </form>
      </Card>
    </div>
  );
}

export default function GiftcodePage() {
  return (
    <AuthGuard redirectTo="/login?redirect=/giftcode">
      <GiftcodeInner />
    </AuthGuard>
  );
}
