import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "VQSV Admin — Vương Quốc Sủng Vật",
  description: "Bảng quản trị game Vương Quốc Sủng Vật",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
