"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";
import { formatDate } from "@/lib/format";

interface GameEvent {
  id: number;
  title: string;
  body: string;
  bannerUrl: string;
  startsAt: string;
  endsAt: string;
  active: boolean;
}

const columns: Column<GameEvent>[] = [
  { key: "id", header: "ID", width: "60px" },
  {
    key: "title",
    header: "Tiêu đề",
    render: (e) => <span className="font-medium text-white">{e.title}</span>,
  },
  { key: "startsAt", header: "Bắt đầu", render: (e) => formatDate(e.startsAt) },
  { key: "endsAt", header: "Kết thúc", render: (e) => formatDate(e.endsAt) },
  {
    key: "active",
    header: "Trạng thái",
    render: (e) => (e.active ? "Đang chạy" : "Tắt"),
  },
];

export default function EventsPage() {
  return (
    <CrudResource<GameEvent>
      title="Sự kiện"
      description="Quản lý sự kiện trong game"
      basePath="/api/admin/events"
      rowKey={(e) => e.id}
      columns={columns}
      createLabel="Tạo sự kiện"
      fields={[
        { name: "title", label: "Tiêu đề", required: true },
        { name: "bannerUrl", label: "Ảnh banner (URL)" },
        { name: "startsAt", label: "Bắt đầu", type: "datetime" },
        { name: "endsAt", label: "Kết thúc", type: "datetime" },
        { name: "body", label: "Nội dung", type: "textarea", required: true },
        { name: "active", label: "Kích hoạt", type: "checkbox", defaultValue: true },
      ]}
    />
  );
}
