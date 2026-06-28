"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";
import { formatNumber, formatVnd } from "@/lib/format";

interface TopupPackage {
  id: number;
  name: string;
  priceVnd: number;
  xuAmount: number;
  bonusXu: number;
  active: boolean;
  sortOrder: number;
}

const columns: Column<TopupPackage>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "name", header: "Tên gói" },
  { key: "priceVnd", header: "Giá", render: (p) => formatVnd(p.priceVnd) },
  { key: "xuAmount", header: "Xu", render: (p) => formatNumber(p.xuAmount) },
  { key: "bonusXu", header: "Xu thưởng", render: (p) => formatNumber(p.bonusXu) },
  {
    key: "active",
    header: "Kích hoạt",
    render: (p) => (p.active ? "Có" : "Không"),
  },
  { key: "sortOrder", header: "Thứ tự" },
];

export default function TopupPackagesPage() {
  return (
    <CrudResource<TopupPackage>
      title="Gói nạp"
      description="Quản lý các gói nạp xu"
      basePath="/api/admin/topup-packages"
      rowKey={(p) => p.id}
      columns={columns}
      editable
      createLabel="Tạo gói nạp"
      fields={[
        { name: "name", label: "Tên gói", required: true },
        { name: "priceVnd", label: "Giá (VND)", type: "number", required: true, defaultValue: 0 },
        { name: "xuAmount", label: "Số xu", type: "number", required: true, defaultValue: 0 },
        { name: "bonusXu", label: "Xu thưởng", type: "number", defaultValue: 0 },
        { name: "sortOrder", label: "Thứ tự", type: "number", defaultValue: 0 },
        { name: "active", label: "Kích hoạt", type: "checkbox", defaultValue: true },
      ]}
    />
  );
}
