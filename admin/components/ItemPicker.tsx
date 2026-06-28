"use client";

import { useEffect, useRef, useState } from "react";
import { apiGet } from "@/lib/api";

export interface GameItem {
  id: number;
  name: string;
  itemType?: string;
}

// Module-level cache so every reward row shares one fetch.
let cache: GameItem[] | null = null;
let inflight: Promise<GameItem[]> | null = null;

function loadItems(): Promise<GameItem[]> {
  if (cache) return Promise.resolve(cache);
  if (!inflight) {
    inflight = apiGet<GameItem[]>("/api/admin/game/items")
      .then((list) => {
        cache = Array.isArray(list) ? list : [];
        return cache;
      })
      .catch((e) => {
        inflight = null; // allow retry
        throw e;
      });
  }
  return inflight;
}

function useItems() {
  const [items, setItems] = useState<GameItem[]>(cache || []);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    let alive = true;
    loadItems()
      .then((list) => alive && setItems(list))
      .catch((e) => alive && setError(e?.message || "Không tải được vật phẩm"));
    return () => {
      alive = false;
    };
  }, []);
  return { items, error };
}

/**
 * Searchable item selector: type an id or a name to filter, then pick.
 * Falls back to a plain numeric id input if the item list can't be loaded.
 */
export default function ItemPicker({
  value,
  onChange,
}: {
  value: number;
  onChange: (id: number) => void;
}) {
  const { items, error } = useItems();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onDocClick);
    return () => document.removeEventListener("mousedown", onDocClick);
  }, []);

  const inputCls =
    "w-full rounded-md border border-surface-border bg-surface px-3 py-2 text-sm text-gray-100 placeholder-gray-500 outline-none focus:border-brand-light focus:ring-1 focus:ring-brand-light";

  // Fallback: list failed to load -> plain numeric id entry so the form still works.
  if (error) {
    return (
      <input
        className={inputCls}
        type="number"
        min={0}
        value={value || ""}
        placeholder="Item ID"
        onChange={(e) => onChange(Number(e.target.value) || 0)}
      />
    );
  }

  const selected = items.find((it) => it.id === value);
  const q = query.trim().toLowerCase();
  const filtered = (q
    ? items.filter(
        (it) =>
          String(it.id).includes(q) || it.name.toLowerCase().includes(q)
      )
    : items
  ).slice(0, 50);

  const display = selected ? `#${selected.id} ${selected.name}` : "";

  return (
    <div className="relative" ref={ref}>
      <input
        className={inputCls}
        value={open ? query : display}
        placeholder={value ? display : "Tìm theo ID hoặc tên..."
        }
        onFocus={() => {
          setQuery("");
          setOpen(true);
        }}
        onChange={(e) => {
          setQuery(e.target.value);
          setOpen(true);
        }}
      />
      {open && (
        <div className="absolute z-20 mt-1 max-h-60 w-full overflow-y-auto rounded-md border border-surface-border bg-surface-card shadow-xl">
          {filtered.length === 0 ? (
            <div className="px-3 py-2 text-xs text-gray-500">
              Không tìm thấy vật phẩm
            </div>
          ) : (
            filtered.map((it) => (
              <button
                key={it.id}
                type="button"
                className={`flex w-full items-center justify-between gap-2 px-3 py-1.5 text-left text-sm hover:bg-surface-hover ${
                  it.id === value ? "bg-surface-hover/60" : ""
                }`}
                onClick={() => {
                  onChange(it.id);
                  setOpen(false);
                }}
              >
                <span className="text-gray-200">{it.name}</span>
                <span className="shrink-0 text-xs text-gray-500">#{it.id}</span>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}
