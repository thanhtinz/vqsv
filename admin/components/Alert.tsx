"use client";

type Kind = "error" | "success" | "info";

const styles: Record<Kind, string> = {
  error: "border-red-500/40 bg-red-500/10 text-red-300",
  success: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
  info: "border-brand-light/40 bg-brand/10 text-brand-light",
};

export default function Alert({
  kind = "info",
  children,
  onClose,
}: {
  kind?: Kind;
  children: React.ReactNode;
  onClose?: () => void;
}) {
  if (!children) return null;
  return (
    <div
      className={`mb-4 flex items-start justify-between gap-3 rounded-md border px-4 py-2.5 text-sm ${styles[kind]}`}
    >
      <div>{children}</div>
      {onClose && (
        <button
          onClick={onClose}
          className="shrink-0 text-lg leading-none opacity-70 hover:opacity-100"
        >
          &times;
        </button>
      )}
    </div>
  );
}
