"use client";

export default function Loading({ label = "Đang tải..." }: { label?: string }) {
  return (
    <div className="flex items-center justify-center py-16 text-gray-400">
      <div className="h-6 w-6 animate-spin rounded-full border-2 border-brand-light border-t-transparent mr-3" />
      <span>{label}</span>
    </div>
  );
}
