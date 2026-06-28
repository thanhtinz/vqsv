"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Card, { SectionTitle } from "@/components/Card";
import Button from "@/components/Button";
import { Select } from "@/components/Input";
import { Loading, ErrorMessage, EmptyState } from "@/components/Loading";
import { apiGet, apiPost, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { formatXu } from "@/lib/format";
import type {
  Account,
  ShopProduct,
  CharacterItem,
  ShopBuyResult,
} from "@/lib/types";

export default function WebshopPage() {
  const { account, isAuthenticated, ready, refreshAccount } = useAuth();

  const [products, setProducts] = useState<ShopProduct[] | null>(null);
  const [loadError, setLoadError] = useState("");

  const [chars, setChars] = useState<CharacterItem[]>([]);
  const [selectedChar, setSelectedChar] = useState("");

  const [buyingId, setBuyingId] = useState<number | null>(null);
  const [feedback, setFeedback] = useState<{
    ok: boolean;
    text: string;
  } | null>(null);

  useEffect(() => {
    apiGet<ShopProduct[]>("/api/web/public/shop/products")
      .then(setProducts)
      .catch((e) =>
        setLoadError(
          e instanceof ApiError ? e.message : "Không tải được sản phẩm."
        )
      );
  }, []);

  useEffect(() => {
    if (!isAuthenticated) return;
    apiGet<CharacterItem[]>("/api/web/profile/characters", { auth: true })
      .then((c) => {
        setChars(c);
        if (c.length > 0) setSelectedChar(String(c[0].id));
      })
      .catch(() => {
        /* ignore */
      });
  }, [isAuthenticated]);

  async function handleBuy(p: ShopProduct) {
    setFeedback(null);
    const char = chars.find((c) => String(c.id) === selectedChar);
    if (!char) {
      setFeedback({ ok: false, text: "Vui lòng chọn nhân vật nhận vật phẩm." });
      return;
    }
    setBuyingId(p.id);
    try {
      const res = await apiPost<ShopBuyResult>(
        "/api/web/shop/buy",
        {
          productId: p.id,
          serverId: char.serverId,
          playerId: char.id,
        },
        { auth: true }
      );
      setFeedback({
        ok: res.success,
        text: res.reward
          ? `${res.message} — Nhận: ${res.reward}`
          : res.message,
      });
      // Refresh balance after a successful purchase.
      if (res.success) {
        try {
          const acc = await apiGet<Account>("/api/web/profile", {
            auth: true,
          });
          if (acc) refreshAccount(acc);
        } catch {
          /* ignore */
        }
      }
    } catch (err) {
      setFeedback({
        ok: false,
        text: err instanceof ApiError ? err.message : "Mua vật phẩm thất bại.",
      });
    } finally {
      setBuyingId(null);
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <SectionTitle
        title="Webshop"
        subtitle="Mua vật phẩm trong game bằng Xu."
        action={
          ready && isAuthenticated && account ? (
            <span className="rounded-lg bg-bg-card px-3 py-1.5 text-sm">
              Số dư:{" "}
              <span className="font-bold text-brand">
                {formatXu(account.balanceXu)}
              </span>
            </span>
          ) : null
        }
      />

      {ready && !isAuthenticated && (
        <Card className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-white/70">
            Đăng nhập để mua vật phẩm và gửi trực tiếp tới nhân vật của bạn.
          </p>
          <Button href="/login?redirect=/webshop" size="sm">
            Đăng nhập
          </Button>
        </Card>
      )}

      {ready && isAuthenticated && (
        <Card className="mb-6">
          {chars.length > 0 ? (
            <div className="w-full sm:max-w-sm">
              <Select
                id="char"
                label="Nhân vật nhận vật phẩm"
                value={selectedChar}
                onChange={(e) => setSelectedChar(e.target.value)}
              >
                {chars.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} (Lv.{c.level}) — {c.serverName}
                  </option>
                ))}
              </Select>
            </div>
          ) : (
            <p className="text-sm text-white/60">
              Bạn chưa có nhân vật nào để nhận vật phẩm.
            </p>
          )}
        </Card>
      )}

      {feedback && (
        <div
          className={`mb-6 rounded-lg border px-4 py-3 text-sm ${
            feedback.ok
              ? "border-emerald-500/40 bg-emerald-500/10 text-emerald-300"
              : "border-red-500/40 bg-red-500/10 text-red-300"
          }`}
        >
          {feedback.text}
        </div>
      )}

      {loadError ? (
        <ErrorMessage message={loadError} />
      ) : !products ? (
        <Loading label="Đang tải sản phẩm..." />
      ) : products.length === 0 ? (
        <EmptyState message="Webshop hiện chưa có sản phẩm." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {products.map((p) => (
            <Card key={p.id} hover className="flex flex-col">
              <div className="mb-3 grid aspect-square place-items-center rounded-lg bg-bg-soft text-3xl text-white/30">
                {p.iconId ? `#${p.iconId}` : "🎁"}
              </div>
              <h3 className="font-bold text-white">{p.name}</h3>
              {p.description && (
                <p className="mt-1 line-clamp-2 text-sm text-white/55">
                  {p.description}
                </p>
              )}
              <p className="mt-2 text-lg font-black text-brand">
                {formatXu(p.priceXu)}
              </p>
              <p className="text-xs text-white/40">
                Kho: {p.stock > 0 ? p.stock : "Hết hàng"}
              </p>
              <div className="mt-auto pt-4">
                {isAuthenticated ? (
                  <Button
                    onClick={() => handleBuy(p)}
                    disabled={
                      buyingId === p.id || p.stock <= 0 || chars.length === 0
                    }
                    className="w-full"
                  >
                    {buyingId === p.id ? "Đang mua..." : "Mua"}
                  </Button>
                ) : (
                  <Link
                    href="/login?redirect=/webshop"
                    className="block w-full rounded-lg border border-white/10 px-4 py-2.5 text-center text-sm font-semibold text-white/70 hover:border-brand/60 hover:text-brand"
                  >
                    Đăng nhập để mua
                  </Link>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
