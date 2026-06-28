"use client";

import { useMemo } from "react";
import { TextInput } from "./FormField";
import Button from "./Button";

/**
 * Structured editor for a reward bundle. Replaces hand-typed JSON: the admin
 * fills in Xu, Vàng and a list of (item, số lượng) rows, and this component
 * serialises to the rewardJson string the server expects, e.g.
 *   {"xu":1000,"gold":50000,"items":[{"itemId":1,"qty":10}]}
 */
export interface RewardValue {
  xu: number;
  gold: number;
  items: { itemId: number; qty: number }[];
}

function parse(value: string | null | undefined): RewardValue {
  if (!value) return { xu: 0, gold: 0, items: [] };
  try {
    const o = typeof value === "string" ? JSON.parse(value) : value;
    return {
      xu: Number(o?.xu) || 0,
      gold: Number(o?.gold) || 0,
      items: Array.isArray(o?.items)
        ? o.items.map((it: any) => ({
            itemId: Number(it?.itemId) || 0,
            qty: Number(it?.qty) || 0,
          }))
        : [],
    };
  } catch {
    return { xu: 0, gold: 0, items: [] };
  }
}

function serialise(v: RewardValue): string {
  return JSON.stringify({
    xu: v.xu || 0,
    gold: v.gold || 0,
    items: v.items.filter((it) => it.itemId > 0 && it.qty > 0),
  });
}

export default function RewardInput({
  value,
  onChange,
}: {
  value: string;
  onChange: (json: string) => void;
}) {
  const reward = useMemo(() => parse(value), [value]);

  function emit(next: RewardValue) {
    onChange(serialise(next));
  }

  return (
    <div className="rounded-md border border-surface-border bg-surface p-3">
      <div className="grid grid-cols-2 gap-3">
        <label className="block">
          <span className="mb-1 block text-[11px] text-gray-400">Xu</span>
          <TextInput
            type="number"
            min={0}
            value={reward.xu}
            onChange={(e) => emit({ ...reward, xu: Number(e.target.value) || 0 })}
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-[11px] text-gray-400">Vàng (gold)</span>
          <TextInput
            type="number"
            min={0}
            value={reward.gold}
            onChange={(e) => emit({ ...reward, gold: Number(e.target.value) || 0 })}
          />
        </label>
      </div>

      <div className="mt-3">
        <div className="mb-1 flex items-center justify-between">
          <span className="text-[11px] text-gray-400">Vật phẩm</span>
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={() =>
              emit({ ...reward, items: [...reward.items, { itemId: 0, qty: 1 }] })
            }
          >
            + Thêm vật phẩm
          </Button>
        </div>

        {reward.items.length === 0 ? (
          <p className="text-[11px] text-gray-500">Chưa có vật phẩm nào.</p>
        ) : (
          <div className="space-y-2">
            {reward.items.map((it, idx) => (
              <div key={idx} className="flex items-center gap-2">
                <label className="flex-1">
                  <span className="mb-1 block text-[10px] text-gray-500">Item ID</span>
                  <TextInput
                    type="number"
                    min={0}
                    value={it.itemId}
                    onChange={(e) => {
                      const items = [...reward.items];
                      items[idx] = { ...it, itemId: Number(e.target.value) || 0 };
                      emit({ ...reward, items });
                    }}
                  />
                </label>
                <label className="flex-1">
                  <span className="mb-1 block text-[10px] text-gray-500">Số lượng</span>
                  <TextInput
                    type="number"
                    min={1}
                    value={it.qty}
                    onChange={(e) => {
                      const items = [...reward.items];
                      items[idx] = { ...it, qty: Number(e.target.value) || 0 };
                      emit({ ...reward, items });
                    }}
                  />
                </label>
                <Button
                  type="button"
                  variant="danger"
                  size="sm"
                  className="mt-4"
                  onClick={() =>
                    emit({
                      ...reward,
                      items: reward.items.filter((_, i) => i !== idx),
                    })
                  }
                >
                  Xoá
                </Button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
