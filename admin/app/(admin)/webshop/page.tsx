"use client";

import CrudResource from "@/components/CrudResource";
import { Column } from "@/components/DataTable";
import { REWARD_HINT, validateRewardJson } from "@/lib/reward";
import { formatNumber } from "@/lib/format";

interface WebshopProduct {
  id: number;
  name: string;
  description: string;
  iconId: number;
  priceXu: number;
  rewardJson: string;
  stock: number;
  active: boolean;
  sortOrder: number;
}

const columns: Column<WebshopProduct>[] = [
  { key: "id", header: "ID", width: "60px" },
  { key: "name", header: "Tên" },
  { key: "iconId", header: "Icon" },
  { key: "priceXu", header: "Giá (xu)", render: (p) => formatNumber(p.priceXu) },
  { key: "stock", header: "Kho", render: (p) => formatNumber(p.stock) },
  { key: "active", header: "Kích hoạt", render: (p) => (p.active ? "Có" : "Không") },
  { key: "sortOrder", header: "Thứ tự" },
];

export default function WebshopPage() {
  return (
    <CrudResource<WebshopProduct>
      title="Webshop"
      description="Quản lý sản phẩm bán bằng xu trên web"
      basePath="/api/admin/webshop-products"
      rowKey={(p) => p.id}
      columns={columns}
      editable
      createLabel="Tạo sản phẩm"
      transformOut={(form) => ({
        ...form,
        rewardJson: validateRewardJson(form.rewardJson),
      })}
      fields={[
        { name: "name", label: "Tên sản phẩm", required: true },
        { name: "description", label: "Mô tả", type: "textarea" },
        { name: "iconId", label: "Icon ID", type: "number", defaultValue: 0 },
        { name: "priceXu", label: "Giá (xu)", type: "number", required: true, defaultValue: 0 },
        { name: "stock", label: "Tồn kho (-1 = vô hạn)", type: "number", defaultValue: -1 },
        { name: "sortOrder", label: "Thứ tự", type: "number", defaultValue: 0 },
        {
          name: "rewardJson",
          label: "Phần thưởng",
          type: "reward",
          required: true,
          hint: REWARD_HINT,
        },
        { name: "active", label: "Kích hoạt", type: "checkbox", defaultValue: true },
      ]}
    />
  );
}
