"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";

interface Warp {
  id: number;
  fromMap: number;
  fromX: number;
  fromY: number;
  toMap: number;
  toX: number;
  toY: number;
}

const columns: Column<Warp>[] = [
  { key: "id", header: "ID", width: "60px" },
  {
    key: "from",
    header: "Từ",
    render: (w) => `BĐ ${w.fromMap} (${w.fromX}, ${w.fromY})`,
  },
  {
    key: "to",
    header: "Đến",
    render: (w) => `BĐ ${w.toMap} (${w.toX}, ${w.toY})`,
  },
];

export default function WarpsPage() {
  return (
    <CrudResource<Warp>
      title="Cổng dịch chuyển"
      description="Quản lý điểm dịch chuyển giữa các bản đồ"
      basePath="/api/admin/game/warps"
      rowKey={(w) => w.id}
      columns={columns}
      createLabel="Tạo cổng"
      fields={[
        { name: "fromMap", label: "Bản đồ nguồn", type: "number", required: true, defaultValue: 0 },
        { name: "fromX", label: "X nguồn", type: "number", defaultValue: 0 },
        { name: "fromY", label: "Y nguồn", type: "number", defaultValue: 0 },
        { name: "toMap", label: "Bản đồ đích", type: "number", required: true, defaultValue: 0 },
        { name: "toX", label: "X đích", type: "number", defaultValue: 0 },
        { name: "toY", label: "Y đích", type: "number", defaultValue: 0 },
      ]}
    />
  );
}
