import Link from "next/link";
import { apiGet } from "@/lib/api";
import Button from "@/components/Button";
import Card, { SectionTitle } from "@/components/Card";
import { EmptyState } from "@/components/Loading";
import { formatVnd, formatXu, formatDate } from "@/lib/format";
import type {
  NewsItem,
  EventItem,
  ServerItem,
  TopupPackage,
} from "@/lib/types";

export const dynamic = "force-dynamic";

// Public data fetched server-side. Any failure (backend offline during dev)
// degrades to an empty list rather than crashing the page.
async function safe<T>(p: Promise<T>, fallback: T): Promise<T> {
  try {
    return await p;
  } catch {
    return fallback;
  }
}

const DOWNLOADS = [
  { label: "Android (APK)", note: "Bản .apk mới nhất" },
  { label: "PC / Windows", note: "Trình giả lập / client PC" },
  { label: "iOS", note: "Hướng dẫn cài đặt" },
  { label: "J2ME", note: "Điện thoại Java cổ điển" },
];

const RELEASES_URL = "https://github.com/";

function serverStatusBadge(status: string) {
  const s = status?.toUpperCase();
  if (s === "ONLINE" || s === "OPEN")
    return "bg-emerald-500/15 text-emerald-400";
  if (s === "MAINTENANCE" || s === "BAOTRI")
    return "bg-amber-500/15 text-amber-400";
  return "bg-white/10 text-white/60";
}

export default async function HomePage() {
  const [news, events, servers, packages] = await Promise.all([
    safe(apiGet<NewsItem[]>("/api/web/public/news?page=0&size=4"), []),
    safe(apiGet<EventItem[]>("/api/web/public/events"), []),
    safe(apiGet<ServerItem[]>("/api/web/public/servers"), []),
    safe(apiGet<TopupPackage[]>("/api/web/public/topup/packages"), []),
  ]);

  const activeEvents = events.filter((e) => e.active).slice(0, 3);
  const featuredPackages = packages.slice(0, 4);

  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden bg-hero-grad">
        <div className="mx-auto max-w-7xl px-4 py-20 text-center sm:py-28">
          <span className="inline-block rounded-full border border-brand/40 bg-brand/10 px-4 py-1 text-xs font-semibold uppercase tracking-widest text-brand">
            MMORPG Sủng Vật
          </span>
          <h1 className="mx-auto mt-6 max-w-3xl text-4xl font-black leading-tight text-white sm:text-6xl">
            Vương Quốc{" "}
            <span className="bg-gradient-to-r from-brand to-accent-light bg-clip-text text-transparent">
              Sủng Vật
            </span>
          </h1>
          <p className="mx-auto mt-5 max-w-2xl text-lg text-white/70">
            Thu phục hàng trăm sủng vật huyền thoại, xây dựng đội hình bất bại
            và chinh phục mọi đấu trường trong thế giới VQSV.
          </p>

          <div className="mt-9 flex flex-wrap justify-center gap-3">
            <Button href="#tai-game" size="lg">
              Tải Game
            </Button>
            <Button href="/nap" size="lg" variant="secondary">
              Nạp Thẻ
            </Button>
            <Button href="/giftcode" size="lg" variant="secondary">
              Giftcode
            </Button>
            <Button href="/bxh" size="lg" variant="ghost">
              Bảng Xếp Hạng
            </Button>
          </div>
        </div>
      </section>

      {/* Download */}
      <section id="tai-game" className="mx-auto max-w-7xl px-4 py-12">
        <SectionTitle
          title="Tải Game"
          subtitle="Chọn nền tảng phù hợp với thiết bị của bạn"
        />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {DOWNLOADS.map((d) => (
            <Card key={d.label} hover>
              <h3 className="text-lg font-bold text-white">{d.label}</h3>
              <p className="mt-1 text-sm text-white/55">{d.note}</p>
              <a
                href={RELEASES_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-4 inline-flex items-center gap-1 text-sm font-semibold text-brand hover:text-brand-light"
              >
                Tải xuống →
              </a>
            </Card>
          ))}
        </div>
        <p className="mt-4 text-xs text-white/40">
          * Các bản tải hiện trỏ tới trang Releases trên GitHub. Liên kết sẽ
          được cập nhật khi có bản phát hành chính thức.
        </p>
      </section>

      {/* News */}
      <section className="mx-auto max-w-7xl px-4 py-12">
        <SectionTitle
          title="Tin Tức Mới Nhất"
          action={
            <Link
              href="/tin-tuc"
              className="text-sm font-semibold text-brand hover:text-brand-light"
            >
              Xem tất cả →
            </Link>
          }
        />
        {news.length === 0 ? (
          <EmptyState message="Chưa có tin tức nào." />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            {news.map((n) => (
              <Link key={n.id} href={`/tin-tuc/${n.slug}`}>
                <Card hover className="h-full overflow-hidden p-0">
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
                  <div className="p-4">
                    {n.category && (
                      <span className="text-xs font-semibold uppercase text-brand">
                        {n.category}
                      </span>
                    )}
                    <h3 className="mt-1 line-clamp-2 font-bold text-white">
                      {n.title}
                    </h3>
                    <p className="mt-1 line-clamp-2 text-sm text-white/55">
                      {n.summary}
                    </p>
                    <p className="mt-2 text-xs text-white/35">
                      {formatDate(n.publishedAt)}
                    </p>
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* Events */}
      <section className="mx-auto max-w-7xl px-4 py-12">
        <SectionTitle
          title="Sự Kiện Đang Diễn Ra"
          action={
            <Link
              href="/su-kien"
              className="text-sm font-semibold text-brand hover:text-brand-light"
            >
              Xem tất cả →
            </Link>
          }
        />
        {activeEvents.length === 0 ? (
          <EmptyState message="Hiện chưa có sự kiện nào đang diễn ra." />
        ) : (
          <div className="grid gap-4 md:grid-cols-3">
            {activeEvents.map((e) => (
              <Card key={e.id} hover className="overflow-hidden p-0">
                <div className="aspect-[2/1] w-full overflow-hidden bg-bg-soft">
                  {e.bannerUrl ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={e.bannerUrl}
                      alt={e.title}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <div className="grid h-full place-items-center text-white/20">
                      Sự kiện
                    </div>
                  )}
                </div>
                <div className="p-4">
                  <h3 className="font-bold text-white">{e.title}</h3>
                  <p className="mt-1 line-clamp-2 text-sm text-white/55">
                    {e.body}
                  </p>
                  <p className="mt-2 text-xs text-white/35">
                    {formatDate(e.startsAt)} — {formatDate(e.endsAt)}
                  </p>
                </div>
              </Card>
            ))}
          </div>
        )}
      </section>

      {/* Servers */}
      <section className="mx-auto max-w-7xl px-4 py-12">
        <SectionTitle title="Trạng Thái Máy Chủ" />
        {servers.length === 0 ? (
          <EmptyState message="Chưa có thông tin máy chủ." />
        ) : (
          <Card className="overflow-x-auto p-0">
            <table className="w-full min-w-[520px] text-left text-sm">
              <thead className="border-b border-white/10 text-white/50">
                <tr>
                  <th className="px-4 py-3 font-medium">Mã</th>
                  <th className="px-4 py-3 font-medium">Tên máy chủ</th>
                  <th className="px-4 py-3 font-medium">Nhóm</th>
                  <th className="px-4 py-3 font-medium">Người chơi</th>
                  <th className="px-4 py-3 font-medium">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {servers.map((s) => (
                  <tr
                    key={s.id}
                    className="border-b border-white/5 last:border-0"
                  >
                    <td className="px-4 py-3 font-mono text-white/80">
                      {s.code}
                    </td>
                    <td className="px-4 py-3 font-medium text-white">
                      {s.name}
                    </td>
                    <td className="px-4 py-3 text-white/60">
                      {s.crossGroup || "-"}
                    </td>
                    <td className="px-4 py-3 text-white/60">
                      {s.playerCount}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full px-2.5 py-1 text-xs font-semibold ${serverStatusBadge(
                          s.status
                        )}`}
                      >
                        {s.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        )}
      </section>

      {/* Featured topup */}
      <section className="mx-auto max-w-7xl px-4 py-12">
        <SectionTitle
          title="Gói Nạp Nổi Bật"
          action={
            <Link
              href="/nap"
              className="text-sm font-semibold text-brand hover:text-brand-light"
            >
              Tất cả gói nạp →
            </Link>
          }
        />
        {featuredPackages.length === 0 ? (
          <EmptyState message="Chưa có gói nạp nào." />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {featuredPackages.map((p) => (
              <Card key={p.id} hover className="text-center">
                <h3 className="font-bold text-white">{p.name}</h3>
                <p className="mt-3 text-2xl font-black text-brand">
                  {formatXu(p.totalXu)}
                </p>
                {p.bonusXu > 0 && (
                  <p className="text-xs text-emerald-400">
                    +{formatXu(p.bonusXu)} thưởng
                  </p>
                )}
                <p className="mt-2 text-sm text-white/60">
                  {formatVnd(p.priceVnd)}
                </p>
                <Button href="/nap" size="sm" className="mt-4 w-full">
                  Nạp ngay
                </Button>
              </Card>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
