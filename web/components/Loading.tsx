export function Loading({ label = "Đang tải..." }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-12 text-white/70">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-brand border-t-transparent" />
      <span>{label}</span>
    </div>
  );
}

export function ErrorMessage({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
      {message}
    </div>
  );
}

export function EmptyState({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-white/10 bg-bg-soft px-4 py-8 text-center text-sm text-white/50">
      {message}
    </div>
  );
}

export default Loading;
