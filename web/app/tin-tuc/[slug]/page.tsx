import Link from "next/link";
import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { apiGet, ApiError } from "@/lib/api";
import { formatDate } from "@/lib/format";
import type { NewsDetail } from "@/lib/types";

export const dynamic = "force-dynamic";

async function getNews(slug: string): Promise<NewsDetail | null> {
  try {
    return await apiGet<NewsDetail>(
      `/api/web/public/news/${encodeURIComponent(slug)}`
    );
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  try {
    const { slug } = await params;
    const news = await getNews(slug);
    if (!news) return { title: "Không tìm thấy tin tức" };
    return {
      title: news.title,
      description: news.summary,
      openGraph: {
        title: news.title,
        description: news.summary,
        images: news.bannerUrl ? [news.bannerUrl] : undefined,
      },
    };
  } catch {
    return { title: "Tin tức" };
  }
}

export default async function NewsDetailPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  let news: NewsDetail | null = null;
  let error = "";
  try {
    const { slug } = await params;
    news = await getNews(slug);
  } catch (e) {
    error = e instanceof Error ? e.message : "Không tải được bài viết.";
  }

  if (!error && !news) notFound();

  return (
    <article className="mx-auto max-w-3xl px-4 py-12">
      <Link
        href="/tin-tuc"
        className="text-sm font-semibold text-brand hover:text-brand-light"
      >
        ← Quay lại tin tức
      </Link>

      {error ? (
        <div className="mt-6 rounded-lg border border-red-500/40 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          {error}
        </div>
      ) : news ? (
        <>
          {news.category && (
            <span className="mt-6 inline-block text-xs font-semibold uppercase text-brand">
              {news.category}
            </span>
          )}
          <h1 className="mt-2 text-3xl font-black leading-tight text-white">
            {news.title}
          </h1>
          <p className="mt-2 text-sm text-white/40">
            {formatDate(news.publishedAt)}
          </p>

          {news.bannerUrl && (
            <div className="mt-6 overflow-hidden rounded-xl bg-bg-soft">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={news.bannerUrl}
                alt={news.title}
                className="w-full object-cover"
              />
            </div>
          )}

          {news.summary && (
            <p className="mt-6 text-lg font-medium text-white/80">
              {news.summary}
            </p>
          )}

          <div
            className="prose-vqsv mt-6"
            dangerouslySetInnerHTML={{ __html: news.body }}
          />
        </>
      ) : null}
    </article>
  );
}
