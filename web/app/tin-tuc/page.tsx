import Link from "next/link";
import { apiGet } from "@/lib/api";
import { SectionTitle } from "@/components/Card";
import { EmptyState } from "@/components/Loading";
import { formatDate } from "@/lib/format";
import type { NewsItem } from "@/lib/types";

export const dynamic = "force-dynamic";

export const metadata = {
  title: "Tin tức",
  description: "Tin tức và cập nhật mới nhất từ Vương Quốc Sủng Vật.",
};

export default async function NewsListPage() {
  let news: NewsItem[] = [];
  let error = "";
  try {
    news = await apiGet<NewsItem[]>("/api/web/public/news?page=0&size=20");
  } catch (e) {
    error = e instanceof Error ? e.message : "Không tải được tin tức.";
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <SectionTitle
        title="Tin Tức"
        subtitle="Tất cả thông báo, cập nhật và bài viết mới nhất."
      />

      {error ? (
        <EmptyState message={error} />
      ) : news.length === 0 ? (
        <EmptyState message="Chưa có tin tức nào." />
      ) : (
        <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
          {news.map((n) => (
            <Link key={n.id} href={`/tin-tuc/${n.slug}`} className="group">
              <article className="flex h-full flex-col overflow-hidden rounded-xl border border-white/10 bg-bg-card transition-colors group-hover:border-brand/50">
                <div className="aspect-video w-full overflow-hidden bg-bg-soft">
                  {n.bannerUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={n.bannerUrl}
                      alt={n.title}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <div className="grid h-full place-items-center text-white/20">
                      VQSV
                    </div>
                  )}
                </div>
                <div className="flex flex-1 flex-col p-4">
                  {n.category && (
                    <span className="text-xs font-semibold uppercase text-brand">
                      {n.category}
                    </span>
                  )}
                  <h2 className="mt-1 line-clamp-2 font-bold text-white">
                    {n.title}
                  </h2>
                  <p className="mt-1 line-clamp-3 flex-1 text-sm text-white/55">
                    {n.summary}
                  </p>
                  <p className="mt-3 text-xs text-white/35">
                    {formatDate(n.publishedAt)}
                  </p>
                </div>
              </article>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
