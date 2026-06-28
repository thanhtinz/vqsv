import { apiGet } from "@/lib/api";
import { SectionTitle } from "@/components/Card";
import { EmptyState } from "@/components/Loading";
import { formatDate } from "@/lib/format";
import type { EventItem } from "@/lib/types";

export const dynamic = "force-dynamic";

export const metadata = {
  title: "Sự kiện",
  description: "Các sự kiện đang diễn ra trong Vương Quốc Sủng Vật.",
};

export default async function EventsPage() {
  let events: EventItem[] = [];
  let error = "";
  try {
    events = await apiGet<EventItem[]>("/api/web/public/events");
  } catch (e) {
    error = e instanceof Error ? e.message : "Không tải được sự kiện.";
  }

  const sorted = [...events].sort(
    (a, b) => Number(b.active) - Number(a.active)
  );

  return (
    <div className="mx-auto max-w-5xl px-4 py-12">
      <SectionTitle
        title="Sự Kiện"
        subtitle="Cập nhật các sự kiện và ưu đãi mới nhất."
      />

      {error ? (
        <EmptyState message={error} />
      ) : sorted.length === 0 ? (
        <EmptyState message="Hiện chưa có sự kiện nào." />
      ) : (
        <div className="flex flex-col gap-6">
          {sorted.map((e) => (
            <article
              key={e.id}
              className="overflow-hidden rounded-xl border border-white/10 bg-bg-card"
            >
              {e.bannerUrl && (
                <div className="aspect-[3/1] w-full overflow-hidden bg-bg-soft">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={e.bannerUrl}
                    alt={e.title}
                    className="h-full w-full object-cover"
                  />
                </div>
              )}
              <div className="p-6">
                <div className="flex flex-wrap items-center gap-3">
                  <h2 className="text-xl font-bold text-white">{e.title}</h2>
                  <span
                    className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                      e.active
                        ? "bg-emerald-500/15 text-emerald-400"
                        : "bg-white/10 text-white/50"
                    }`}
                  >
                    {e.active ? "Đang diễn ra" : "Đã kết thúc"}
                  </span>
                </div>
                <p className="mt-1 text-xs text-white/40">
                  {formatDate(e.startsAt)} — {formatDate(e.endsAt)}
                </p>
                <div className="prose-vqsv mt-3 whitespace-pre-line text-sm">
                  {e.body}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
