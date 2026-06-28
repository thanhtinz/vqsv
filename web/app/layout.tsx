import type { Metadata } from "next";
import "./globals.css";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";

export const metadata: Metadata = {
  title: {
    default: "Vương Quốc Sủng Vật (VQSV) — Trang chủ chính thức",
    template: "%s | VQSV",
  },
  description:
    "Trang chủ chính thức của game Vương Quốc Sủng Vật (VQSV). Tải game, nạp thẻ, nhận giftcode, xem bảng xếp hạng, tin tức và sự kiện mới nhất.",
  keywords: [
    "VQSV",
    "Vương Quốc Sủng Vật",
    "game sủng vật",
    "nạp thẻ",
    "giftcode",
  ],
  openGraph: {
    title: "Vương Quốc Sủng Vật (VQSV)",
    description:
      "Thế giới sủng vật huyền ảo trong tầm tay. Tải game và bắt đầu hành trình chinh phục.",
    type: "website",
    locale: "vi_VN",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi">
      <body className="flex min-h-screen flex-col bg-bg font-sans">
        <Navbar />
        <main className="flex-1">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
