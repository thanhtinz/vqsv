"use client";

import { ReactNode } from "react";

interface StatCardProps {
  label: string;
  value: ReactNode;
  accent?: string;
  hint?: string;
}

export default function StatCard({
  label,
  value,
  accent = "text-brand-light",
  hint,
}: StatCardProps) {
  return (
    <div className="rounded-xl border border-surface-border bg-surface-card p-5">
      <div className="text-xs font-medium uppercase tracking-wide text-gray-400">
        {label}
      </div>
      <div className={`mt-2 text-2xl font-bold ${accent}`}>{value}</div>
      {hint && <div className="mt-1 text-xs text-gray-500">{hint}</div>}
    </div>
  );
}
