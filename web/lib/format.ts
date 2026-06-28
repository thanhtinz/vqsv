// Small formatting helpers (Vietnamese locale).

export function formatVnd(value: number): string {
  return new Intl.NumberFormat("vi-VN").format(value || 0) + "đ";
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat("vi-VN").format(value || 0);
}

export function formatXu(value: number): string {
  return formatNumber(value) + " Xu";
}

export function formatDate(input?: string | null): string {
  if (!input) return "";
  const d = new Date(input);
  if (Number.isNaN(d.getTime())) return input;
  return d.toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function formatDateTime(input?: string | null): string {
  if (!input) return "";
  const d = new Date(input);
  if (Number.isNaN(d.getTime())) return input;
  return d.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
