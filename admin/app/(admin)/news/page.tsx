"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";
import { formatDate } from "@/lib/format";

interface News {
  id: number;
  title: string;
  slug: string;
  summary: string;
  body: string;
  bannerUrl: string;
  category: string;
  published: boolean;
  author: string;
  publishedAt: string;
  createdAt: string;
}

const columns: Column<News>[] = [
  { key: "id", header: "ID", width: "60px" },
  {
    key: "title",
    header: "Tiêu đề",
    render: (n) => <span className="font-medium text-white">{n.title}</span>,
  },
  { key: "category", header: "Chuyên mục" },
  {
    key: "published",
    header: "Trạng thái",
    render: (n) =>
      n.published ? (
        <span className="rounded bg-emerald-500/20 px-2 py-0.5 text-xs text-emerald-300">
          Đã đăng
        </span>
      ) : (
        <span className="rounded bg-gray-500/20 px-2 py-0.5 text-xs text-gray-300">
          Nháp
        </span>
      ),
  },
  { key: "author", header: "Tác giả" },
  { key: "createdAt", header: "Tạo lúc", render: (n) => formatDate(n.createdAt) },
];

export default function NewsPage() {
  return (
    <CrudResource<News>
      title="Tin tức"
      description="Quản lý bài viết tin tức"
      basePath="/api/admin/news"
      rowKey={(n) => n.id}
      columns={columns}
      createLabel="Tạo tin tức"
      fields={[
        { name: "title", label: "Tiêu đề", required: true },
        { name: "slug", label: "Slug (tuỳ chọn)", hint: "Để trống sẽ tự tạo từ tiêu đề" },
        { name: "category", label: "Chuyên mục", placeholder: "vd: cap-nhat, su-kien" },
        { name: "bannerUrl", label: "Ảnh banner (URL)" },
        { name: "author", label: "Tác giả" },
        { name: "summary", label: "Tóm tắt", type: "textarea" },
        { name: "body", label: "Nội dung", type: "textarea", required: true },
        { name: "published", label: "Đăng công khai", type: "checkbox", defaultValue: false },
      ]}
    />
  );
}
